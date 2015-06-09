package br.org.funcate.terramobile.controller.activity.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import br.org.funcate.terramobile.R;
import br.org.funcate.terramobile.controller.activity.MainActivity;
import br.org.funcate.terramobile.util.Message;

/**
 * This AsyncTask downloads a geopackage from the server
 */
public class DownloadTask extends AsyncTask<String, String, Boolean> {
    private String unzipDestinationFilePath;
    private String downloadDestinationFilePath;
    private ArrayList<String> mFiles;

    private boolean overwrite;

    private MainActivity mainActivity;

    private File destinationFile;

    public DownloadTask(String downloadDestinationFilePath, String unzipDestinationFilePath, boolean overwrite, MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.unzipDestinationFilePath = unzipDestinationFilePath;
        this.downloadDestinationFilePath = downloadDestinationFilePath;
        this.overwrite = overwrite;
    }

    @Override
    protected void onPreExecute() {
        mainActivity.showProgressDialog(mainActivity.getString(R.string.downloading));
    }

    protected Boolean doInBackground(String... urlToDownload) {
        if(android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();

        if (urlToDownload[0].isEmpty()) {
            Log.e("URL missing", "Variable urlToDownload[0] is empty");
            return false;
        }

        if (downloadDestinationFilePath.isEmpty()) {
            Log.e("Path missing", "Variable downloadDestinationFilePath is empty");
            return false;
        }
        destinationFile = new File(downloadDestinationFilePath);
        try {
            if (!destinationFile.exists()) {
                destinationFile.createNewFile();
            } else {
                if (overwrite) {
                    destinationFile.delete();
                } else {
                    return true;
                }
            }
            URL url = new URL(urlToDownload[0]);
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();

            int totalSize = urlConnection.getContentLength();

            InputStream inputStream = new BufferedInputStream(url.openStream());
            OutputStream fileOutput = new FileOutputStream(destinationFile);

            byte buffer[] = new byte[1024];

            int bufferLength;
            long total = 0;
//                if(android.os.Debug.isDebuggerConnected()) android.os.Debug.waitForDebugger(); Para debugar é preciso colocar um breakpoint nessa linha
            while ((bufferLength = inputStream.read(buffer)) != -1) {
                if(isCancelled()) {
                    fileOutput.flush();
                    fileOutput.close();
                    inputStream.close();
                    return false;
                }
                total += bufferLength;
                publishProgress("" + (int) ((total * 100) / totalSize), mainActivity.getResources().getString(R.string.downloading));
                fileOutput.write(buffer, 0, bufferLength);
            }
            fileOutput.flush();
            fileOutput.close();
            inputStream.close();

            mFiles = this.unzip(new File(downloadDestinationFilePath), new File(unzipDestinationFilePath));
            return true;
        }catch (IOException e) {
            e.printStackTrace();
            if(destinationFile.exists())
                destinationFile.delete();
            return false;
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            if(destinationFile.exists())
                destinationFile.delete();
            return false;
        }
    }

    /**
     * Called when the cancel button of the ProgressDialog is touched
     * @param aBoolean
     */
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        if(destinationFile.exists())
            destinationFile.delete();
        Message.showSuccessMessage(mainActivity, R.string.success, R.string.download_cancelled);
    }

    /**
     * Count the number of files on a zip
     * @param zipFile Zip file
     * @return The number of files on the zip archive
     */
    private long countZipFiles(File zipFile){
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(zipFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        long totalFiles = 0;
        try {
            if(zis != null) {
                while (zis.getNextEntry() != null) {
                    totalFiles++;
                }
                zis.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalFiles;
    }

    /**
     * Unzip an archive
     * @param zipFile Zip archive
     * @param targetDirectory Directory to unzip the files
     * @throws IOException
     */
    public ArrayList<String> unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));

        ArrayList<String> files=new ArrayList<String>();

        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            int numFiles = 0;
            long totalFiles = countZipFiles(zipFile);

            while ((ze = zis.getNextEntry()) != null) {
                numFiles++;

                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                files.add(ze.getName());
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    long total = 0;
                    long totalZipSize = ze.getCompressedSize();
                    while ((count = zis.read(buffer)) != -1) {
                        total += count;
                        publishProgress("" + (int) ((total * 100) / totalZipSize), mainActivity.getResources().getString(R.string.decompressing)+"\n"+mainActivity.getResources().getString(R.string.file) + " " + numFiles + "/" + totalFiles);
                        fout.write(buffer, 0, count);
                    }
                } finally {
                    fout.close();
                }
            }
        } finally {
            zis.close();
        }
        return files;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        mainActivity.getTreeView().refreshTreeView();
        // The project is the last downloaded geopackage file.
        mainActivity.getProject().setCurrent(mFiles.get(0));

        if(mainActivity.getProgressDialog() != null && mainActivity.getProgressDialog().isShowing()) {
            if (aBoolean) {
                mainActivity.getProgressDialog().dismiss();
                mainActivity.getListPackageFragment().dismiss();
                Message.showSuccessMessage(mainActivity, R.string.success, R.string.download_success);
            } else {
                mainActivity.getProgressDialog().dismiss();
                Message.showErrorMessage(mainActivity, R.string.error, R.string.download_failed);
            }
        }
        else{
            Message.showErrorMessage(mainActivity, R.string.error, R.string.download_failed);
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if(mainActivity.getProgressDialog() != null && mainActivity.getProgressDialog().isShowing()) {
            mainActivity.getProgressDialog().setProgress(Integer.parseInt(values[0]));
            mainActivity.getProgressDialog().setMessage(values[1]);
        }
    }
}