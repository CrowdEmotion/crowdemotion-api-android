import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import com.crowdemotion.api.client.CEClient;
import java.io.File;


public class AndroidRestAPIClientTest extends Activity {

    /* ACTIVITY NOT COMPLETE */

    public void onTestClick(View view) {

        final CEClient ceClient = new CEClient();

        CEClient.CECallback cb = new CEClient.CECallback(){

            public void execute(boolean result, Object obj) {
                Log.e("CECLIENT caller", ""+result);
                Log.e("CECLIENT obj", obj != null ? obj.toString() : "(null)");


                // *** upload a file using an URL that will be used by CE backend to download the file
                //ceClient.uploadLink("http://example.com/movies/video.mp4", null);


                // *** direct upload of a video file
                final String mediaDir = "CrowdEmotionMedia", fName = "video.mp4";
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES), mediaDir);
                String fFullpath = mediaStorageDir.getPath() + File.separator + fName;

                ceClient.upload(fFullpath, new CEClient.CECallback(){
                    public void execute(boolean result, Object obj) {
                        if(result)
                            Log.e("CECLIENT", "video uploaded!");
                        else
                            Log.e("CECLIENT", "video not uploaded");
                    }
                });


                // *** get back the results for a certain response id
                //ceClient.findArray(2817L, new Long[]{1L,2L,3L,4L,5L,6L,7L,8L}, null);

            }
        };

        ceClient.login("username", "password", cb);

    }
}
