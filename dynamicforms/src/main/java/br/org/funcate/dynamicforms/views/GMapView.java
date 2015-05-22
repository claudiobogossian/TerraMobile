/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.org.funcate.dynamicforms.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import br.org.funcate.dynamicforms.R;
import br.org.funcate.dynamicforms.images.ImageUtilities;
import br.org.funcate.dynamicforms.util.ResourcesManager;

import static br.org.funcate.dynamicforms.FormUtilities.COLON;
import static br.org.funcate.dynamicforms.FormUtilities.UNDERSCORE;

/**
 * A custom map view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GMapView extends View implements GView {

    private LinearLayout mainLayout;
    private String value;

    /**
     * @param context  the context to use.
     * @param attrs    attributes.
     * @param defStyle def style.
     */
    public GMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param context the context to use.
     * @param attrs   attributes.
     */
    public GMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context               the context to use.
     * @param attrs                 attributes.
     * @param parentView            parent
     * @param label                   label
     * @param value                 value
     * @param constraintDescription constraints
     */
    public GMapView(final Context context, AttributeSet attrs, LinearLayout parentView, String label, String value,
                    String constraintDescription) {
        super(context, attrs);
        this.value = value;

/*        try {
            mainLayout = new LinearLayout(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(10, 10, 10, 10);
            mainLayout.setLayoutParams(layoutParams);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            parentView.addView(mainLayout);

            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            textView.setPadding(2, 2, 2, 2);
            textView.setText(label.replace(UNDERSCORE, " ").replace(COLON, " ") + " " + constraintDescription);
            textView.setTextColor(context.getResources().getColor(R.color.formcolor));
            mainLayout.addView(textView);

            final long imageId = Long.parseLong(value.trim());

            final IImagesDbHelper imagesDbHelper = DefaultHelperClasses.getDefaulfImageHelper();

            byte[] imageThumbnail = imagesDbHelper.getImageThumbnail(imageId);
            Bitmap thumbnail = ImageUtilities.getImageFromImageData(imageThumbnail);

            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(102, 102));
            imageView.setPadding(5, 5, 5, 5);
            imageView.setImageBitmap(thumbnail);
            imageView.setBackgroundDrawable(getResources().getDrawable(R.drawable.border_black_1px));
            imageView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // the old way
                    // Intent intent = new Intent();
                    // intent.setAction(android.content.Intent.ACTION_VIEW);
                    //                    intent.setDataAndType(Uri.fromFile(image), "image*//*"); //$NON-NLS-1$
                    // context.startActivity(intent);
//
//                        *//*
//                         * open in markers to edit it
//                         *//*
//                    MarkersUtilities.launchOnImage(context, image);

                    try {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        Image image = imagesDbHelper.getImage(imageId);
                        File tempDir = ResourcesManager.getInstance(context).getTempDir();
                        String ext = ".jpg";
                        if (image.getName().endsWith(".png"))
                            ext = ".png";
                        File imageFile = new File(tempDir, ImageUtilities.getTempImageName(ext));
                        byte[] imageData = imagesDbHelper.getImageData(image.getId());
                        ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());

                        intent.setDataAndType(Uri.fromFile(imageFile), "image*//*"); //$NON-NLS-1$
                        context.startActivity(intent);
                    } catch (Exception e) {
                        //GPLog.error(this, null, e);
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

            });
            mainLayout.addView(imageView);
        } catch (Exception e) {
            //GPLog.error(this, null, e);
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }*/
        Toast.makeText(context, "revisar implementacao do construtor em GMapView", Toast.LENGTH_LONG).show();
    }

    public void refresh(final Context context) {
        // ignore
    }

    public String getValue() {
        return value;
    }

    @Override
    public void setOnActivityResult(Intent data) {
        // ignore
    }

}