/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 * <p>
 * Amended by Augmented Technologies Ltd to accept both OGC/ISO WKB encoding as well
 * as PostGIS EWKB
 * <p>
 */
package com.augtech.geoapi.geopackage.geometry;

import java.io.IOException;

import com.augtech.geoapi.geopackage.GeoPackage;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ByteArrayInStream;
import com.vividsolutions.jts.io.ByteOrderDataInStream;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.InStream;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBConstants;
import com.vividsolutions.jts.io.WKBReader;
/**
 * Reads a {@link Geometry}from a byte stream in to OGC Well-Known Binary format.
 * Supports use of an {@link InStream}, which allows easy use
 * with arbitary byte stream sources.
 * <p>
 * This class reads the format describe in {@link OGCWKBWriter}.  It also partially handles
 * the Extended WKB format used by PostGIS (by reading SRID values)
 * <p>
 * This is a direct copy of the JTS {@link WKBReader} with amendments to support reading
 * ISO/ OGC WKB format, as written by {@link OGCWKBWriter} as well as the PostGIS EWKB 
 * Geometry - The primary difference is in {@link #readGeometry()} where the geometry type
 * is decoded.
 *
 * @see OGCWKBWriter
 */
public class OGCWKBReader {
	/**
	 * Converts a hexadecimal string to a byte array.
	 *
	 * @param hex a string containing hex digits
	 */
	public static byte[] hexToBytes(String hex)
	{
		int byteLen = hex.length() / 2;
		byte[] bytes = new byte[byteLen];

		for (int i = 0; i < hex.length() / 2; i++) {
			int i2 = 2 * i;
			if (i2 + 1 > hex.length())
				throw new IllegalArgumentException("Hex string has odd length");

			int nib1 = hexToInt(hex.charAt(i2));
			int nib0 = hexToInt(hex.charAt(i2 + 1));
			byte b = (byte) ((nib1 << 4) + (byte) nib0);
			bytes[i] = b;
		}
		return bytes;
	}

	private static int hexToInt(char hex)
	{
		int nib = Character.digit(hex, 16);
		if (nib < 0)
			throw new IllegalArgumentException("Invalid hex digit");
		return nib;
	}

	private static final String INVALID_GEOM_TYPE_MSG
	= "Invalid geometry type encountered in ";

	private GeometryFactory factory;
	private PrecisionModel precisionModel;
	// default dimension - will be set on read
	private int inputDimension = 2;
	private boolean hasSRID = false;
	private int SRID = 0;
	private ByteOrderDataInStream dis = new ByteOrderDataInStream();
	private double[] ordValues;

	public OGCWKBReader() {
		this(new GeometryFactory());
	}

	public OGCWKBReader(GeometryFactory geometryFactory) {
		this.factory = geometryFactory;
		precisionModel = factory.getPrecisionModel();
	}

	/**
	 * Reads a single {@link Geometry} from a byte array.
	 *
	 * @param bytes the byte array to read from
	 * @return the geometry read
	 * @throws ParseException if a parse exception occurs
	 */
	public Geometry read(byte[] bytes) throws ParseException  {
		// possibly reuse the ByteArrayInStream?
		// don't throw IOExceptions, since we are not doing any I/O
		try {
			return read(new ByteArrayInStream(bytes));
		}
		catch (IOException ex) {
			throw new RuntimeException("Unexpected IOException caught: " + ex.getMessage());
		}
	}

	/**
	 * Reads a {@link Geometry} from an {@link InStream).
	 *
	 * @param is the stream to read from
	 * @return the Geometry read
	 * @throws IOException
	 * @throws ParseException
	 */
	public Geometry read(InStream is) throws IOException, ParseException  {
		dis.setInStream(is);
		Geometry g = readGeometry();
		setSRID(g);
		return g;
	}

	private Geometry readGeometry() throws IOException, ParseException {
		// determine byte order
		byte byteOrder = dis.readByte();
		// default is big endian
		if (byteOrder == WKBConstants.wkbNDR)
			dis.setOrder(ByteOrderValues.LITTLE_ENDIAN);

		/*      EWKB          		 OGC
		Z     base | 0x80000000    base + 1000
		M     base | 0x40000000    base + 2000
		ZM    base | 0xC0000000    base + 3000
		 */
		int typeInt = dis.readInt();
		int geometryType = 1;
		
		if (typeInt % 1000 >0) {
			
			// OGC/ISO
			geometryType = typeInt % 1000;
			int dim = (typeInt-geometryType) / 1000;
			inputDimension = (dim==1 || dim==2) ? 3 : 2;
			if (dim==3) throw new IllegalArgumentException("4 dimensional data is not supported");
			
		} else {
			
			if (GeoPackage.MODE_STRICT)
				throw new IOException("EWKB found in GeoPackage");
			
			// PostGIS EWKB
			geometryType = typeInt & 0xff;
			inputDimension = ((typeInt & 0x80000000) != 0) ? 3 : 2;
			hasSRID = (typeInt & 0x20000000) != 0;

		}

		if (hasSRID) {
			SRID = dis.readInt();
		}

		// only allocate ordValues buffer if necessary
		if (ordValues == null || ordValues.length < inputDimension)
			ordValues = new double[inputDimension];

		switch (geometryType) {
		case WKBConstants.wkbPoint :
			return readPoint();
		case WKBConstants.wkbLineString :
			return readLineString();
		case WKBConstants.wkbPolygon :
			return readPolygon();
		case WKBConstants.wkbMultiPoint :
			return readMultiPoint();
		case WKBConstants.wkbMultiLineString :
			return readMultiLineString();
		case WKBConstants.wkbMultiPolygon :
			return readMultiPolygon();
		case WKBConstants.wkbGeometryCollection :
			return readGeometryCollection();
		}
		throw new ParseException("Unknown WKB type " + geometryType);
		//return null;
	}

	/**
	 * Sets the SRID, if it was specified in the WKB
	 *
	 * @param g the geometry to update
	 * @return the geometry with an updated SRID value, if required
	 */
	private Geometry setSRID(Geometry g)  {
		if (SRID != 0)
			g.setSRID(SRID);
		return g;
	}

	private Point readPoint() throws IOException  {
		CoordinateSequence pts = readCoordinateSequence(1);
		return factory.createPoint(pts);
	}

	private LineString readLineString() throws IOException {
		int size = dis.readInt();
		CoordinateSequence pts = readCoordinateSequence(size);
		return factory.createLineString(pts);
	}

	private LinearRing readLinearRing() throws IOException  {
		int size = dis.readInt();
		CoordinateSequence pts = readCoordinateSequence(size);
		return factory.createLinearRing(pts);
	}

	private Polygon readPolygon() throws IOException  {
		int numRings = dis.readInt();
		LinearRing[] holes = null;
		if (numRings > 1)
			holes = new LinearRing[numRings - 1];

		LinearRing shell = readLinearRing();
		for (int i = 0; i < numRings - 1; i++) {
			holes[i] = readLinearRing();
		}
		return factory.createPolygon(shell, holes);
	}

	private MultiPoint readMultiPoint() throws IOException, ParseException  {
		int numGeom = dis.readInt();
		Point[] geoms = new Point[numGeom];
		for (int i = 0; i < numGeom; i++) {
			Geometry g = readGeometry();
			if (! (g instanceof Point))
				throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiPoint");
			geoms[i] = (Point) g;
		}
		return factory.createMultiPoint(geoms);
	}

	private MultiLineString readMultiLineString() throws IOException, ParseException  {
		int numGeom = dis.readInt();
		LineString[] geoms = new LineString[numGeom];
		for (int i = 0; i < numGeom; i++) {
			Geometry g = readGeometry();
			if (! (g instanceof LineString))
				throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiLineString");
			geoms[i] = (LineString) g;
		}
		return factory.createMultiLineString(geoms);
	}

	private MultiPolygon readMultiPolygon() throws IOException, ParseException {
		int numGeom = dis.readInt();
		Polygon[] geoms = new Polygon[numGeom];
		for (int i = 0; i < numGeom; i++) {
			Geometry g = readGeometry();
			if (! (g instanceof Polygon))
				throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiPolygon");
			geoms[i] = (Polygon) g;
		}
		return factory.createMultiPolygon(geoms);
	}

	private GeometryCollection readGeometryCollection() throws IOException, ParseException {
		int numGeom = dis.readInt();
		Geometry[] geoms = new Geometry[numGeom];
		for (int i = 0; i < numGeom; i++) {
			geoms[i] = readGeometry();
		}
		return factory.createGeometryCollection(geoms);
	}

	private CoordinateSequence readCoordinateSequence(int size) throws IOException {
		CoordinateSequence seq = factory.getCoordinateSequenceFactory().create(size, inputDimension);
		int targetDim = seq.getDimension();
		if (targetDim > inputDimension)
			targetDim = inputDimension;
		for (int i = 0; i < size; i++) {
			readCoordinate();
			for (int j = 0; j < targetDim; j++) {
				seq.setOrdinate(i, j, ordValues[j]);
			}
		}
		return seq;
	}

	/**
	 * Reads a coordinate value with the specified dimensionality.
	 * Makes the X and Y ordinates precise according to the precision model
	 * in use.
	 */
	private void readCoordinate() throws IOException {
		for (int i = 0; i < inputDimension; i++) {
			if (i <= 1) {
				ordValues[i] = precisionModel.makePrecise(dis.readDouble());
			}
			else {
				ordValues[i] = dis.readDouble();
			}

		}
	}
}
