package com.crowdemotion.api.client;

import android.os.AsyncTask;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import us.monoid.web.AbstractContent;
import us.monoid.web.Content;
import us.monoid.web.JSONResource;
import us.monoid.web.Replacement;
import us.monoid.web.Resty;


public class CEClient {

    private static final String VERSION_URI = "/v1";
    private static final String BASE_URI = "https://api.crowdemotion.co.uk" + VERSION_URI;

    private SecureRandom random = new SecureRandom();

    protected Resty _r;

    protected String _userId = null;
    protected String _token = null;


    protected interface Callback {
        AbstractContent getPayload() throws Exception;
        void execute(JSONResource json);
    }

    public interface CECallback {
        void execute(boolean result, Object obj);
    }


    public CEClient() {
        _r = new Resty();
    }


    /**
     * Call this method to obtain login tokens for subsequent calls
     * @param username
     * @param password
     * @param cecb if not null, execute() method will be called with result = true/false and obj = null
     */
    public void login(final String username, final String password, final CECallback cecb) {

        String entity = "user", function = "login";
        String url = entity + "/" + function;

        Callback cb = new Callback(){

            public AbstractContent getPayload() throws Exception {
                JSONObject creds = new JSONObject();
                creds.put("username", username);
                creds.put("password", password);
                return Resty.content(creds);
            }

            public void execute(JSONResource jsonResource) {
                boolean result = false;
                try {
                    JSONObject json = jsonResource.toObject();
                    _userId = (String)json.get("userId");
                    _token = (String)json.get("token");
                    if(_userId != null && _token != null && _userId.length() > 0 && _token.length() > 0)
                        result = true;
                } catch (Exception e) {}
                if(cecb != null)
                    cecb.execute(result, null);
            }
        };

        new RetrieveDataTask(cb, false).execute(url);
    }

    public static class FaceVideo {
        private String remoteLocation;
        private String celocation;
        private Long id;
        private String fileName;
        private Long responseId;
        private Integer status;
        private Double frameRate;
        private Integer numFrames;
    }

    protected static FaceVideo FaceVideoFromJSON(JSONResource jsonResource) {
        FaceVideo fv = null;
        try {
            JSONObject json = jsonResource.toObject();

            if(json != null && json.length() > 0) {
                fv = new FaceVideo();
                fv.remoteLocation =  json.isNull("remoteLocation") ? null : (String)json.get("remoteLocation");
                fv.celocation = json.isNull("celocation") ? null : (String)json.get("celocation");
                fv.id = ((Number)json.get("id")).longValue();
                fv.fileName = (String)json.get("fileName");
                fv.responseId = ((Number)json.get("responseId")).longValue();
                fv.status = (Integer)json.get("status");
                fv.frameRate = json.isNull("frameRate") ? null : (Double)json.get("frameRate");
                fv.numFrames = json.isNull("numFrames") ? null : (Integer)json.get("numFrames");
            }
        } catch (Exception e) {
        }

        return fv;
    }

    /**
     * Upload a file using an URL that will be used by CE backend to download the file
     * @param link the complete URL of a video file reachable from the Internet
     * @param cecb if not null, execute() method will be called with result = true/false and obj = FaceVideo instance
     */
    public void uploadLink(final String link, final CECallback cecb) {

        String entity = "facevideo";
        String url = entity;

        Callback cb = new Callback(){

            public AbstractContent getPayload() throws Exception {
                JSONObject payload = new JSONObject();
                payload.put("link", link);
                return Resty.content(payload);
            }

            public void execute(JSONResource jsonResource) {
                FaceVideo fv = FaceVideoFromJSON(jsonResource);
                if(cecb != null)
                    cecb.execute(fv != null, fv);
            }
        };

        new RetrieveDataTask(cb, true).execute(url);
    }

    /**
     * direct upload of a video file
     * @param path complete file path on device storage
     * @param cecb if not null, execute() method will be called with result = true/false and obj = FaceVideo instance
     */
    public void upload(final String path, final CECallback cecb) {

        final File file = new File(path);
        String entity = "facevideo", function = "upload";
        String url = entity +"/"+ function  +"/"+ file.getName();

        Callback cb = new Callback(){

            public AbstractContent getPayload() throws Exception {

                // TODO be more efficient space-wise
                byte[] fileData = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(fileData);
                fis.close();

                AbstractContent payload = Resty.put(new Content("application/json", fileData));

                return payload;
            }

            public void execute(JSONResource jsonResource) {
                FaceVideo fv = FaceVideoFromJSON(jsonResource);

                if(cecb != null)
                    cecb.execute(jsonResource != null, null);
            }
        };

        new RetrieveDataTask(cb, true).execute(url);
    }

    /*
    public FaceVideo upload_form(String path) {

    }

    public boolean writeArray(Long responseId, Long metric_id, List<Number> data) {

    }
    */

    public class ResultArray {
        private Long responseId;
        private String metricName;
        private Integer startIndex;
        private Integer endIndex;
        private Integer stepSize;
        private List<Number> data = new ArrayList<Number>();
    }

    public void findArray(Long response_id, Long[] metric_ids, final CECallback cecb) {
        String entity = "timeseries";
        String url = entity + "?response_id=" + response_id;

        for(Long metric_id : metric_ids) {
            url += "&metric_id=" + metric_id;
        }

        Callback cb = new Callback(){

            public AbstractContent getPayload() throws Exception {
                return null;
            }

            public void execute(JSONResource jsonResource) {
                ResultArray[] ras = null;

                if (jsonResource != null && jsonResource.status(200)) {

                    try {
                        JSONArray jsonArray = jsonResource.array();

                        ras = new ResultArray[jsonArray.length()];

                        for(int i=0; i < jsonArray.length(); i++) {
                            JSONObject json = jsonArray.getJSONObject(i);

                            ResultArray ra = new ResultArray();
                            ras[i] = ra;

                            ra.responseId = json.getLong("responseId");
                            ra.metricName = json.getString("metricName");
                            ra.startIndex = json.getInt("startIndex");
                            ra.endIndex = json.getInt("endIndex");
                            ra.stepSize = json.getInt("stepSize");

                            JSONArray jsonData = json.getJSONArray("data");
                            if(jsonData != null && jsonData.length() >= 0) {
                                ra.data = new ArrayList<Number>();
                                for(int j=0; j < jsonData.length(); j++) {
                                    ra.data.add((Number)jsonData.get(j));
                                }
                            }

                        }

                    } catch(Exception e){
                        ras = null;
                    }
                }

                if(cecb != null)
                    cecb.execute(ras != null, ras);
            }
        };

        new RetrieveDataTask(cb, true).execute(url);
    }


    /* ************** */

    protected class RetrieveDataTask extends AsyncTask<String, Void, JSONResource> {

        private Callback cb;
        private boolean isAuthed;
        private Exception exception;

        public RetrieveDataTask(Callback cb, boolean isAuthed) {
            this.cb = cb;
            this.isAuthed = isAuthed;
        }

        protected JSONResource doInBackground(String... urls) {

            int errn=0;
            while(++errn <= 1) {
                try {

                    JSONResource json = null;
                    String url = urls[0];

                    AbstractContent payload = cb.getPayload();
                    boolean isGet = false, isPut = false;
                    if(payload == null) {
                        isGet = true;
                    } else if(payload instanceof Replacement) {
                        isPut = true;
                    }

                    if(isAuthed) {
                        String function = url;
                        if(function.indexOf('?') >= 0)
                            function = function.split("\\?")[0];
                        addAuthHeaders(_r, function, isGet ? "GET" : (isPut ? "PUT" : "POST"));
                    }

                    url = BASE_URI +"/" + url;

                    if(isGet)
                        json = _r.json(url);
                    else
                        json = _r.json(url, payload);

                    return json;

                } catch (Exception e) {
                    //e.printStackTrace();
                    this.exception = e;
                }
            }

            return null;
        }

        protected void onPostExecute(JSONResource json) {

            // TODO: check/pass this.exception

            cb.execute(json);

        }

    }

    protected void addAuthHeaders(Resty r, String path, String httpMethod) {

        path = (VERSION_URI +"/"+ path).substring(1);

        String time = getISO8601StringForDate(new Date());
        String nonce = generateNonce();
        String authToken = calculateAuthToken(_token, path + "," + httpMethod, nonce, time);

        r.withHeader("Authorization", _userId + ":" + authToken);
        r.withHeader("x-ce-rest-date", time);
        r.withHeader("nonce", nonce);
    }

    private static String getISO8601StringForDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    private String generateNonce() {
        return new BigInteger(130, random).toString(32);
    }

    protected String calculateAuthToken(String sessionToken, String stringToHash, String nonce, String time) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((sessionToken + ":" + stringToHash + "," + time +"," + nonce).getBytes("UTF-8"));
            return Base64.encodeToString(hash, 0);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
