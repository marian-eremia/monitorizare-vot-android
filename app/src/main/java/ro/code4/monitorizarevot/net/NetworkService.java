package ro.code4.monitorizarevot.net;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.realm.RealmObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ro.code4.monitorizarevot.BuildConfig;
import ro.code4.monitorizarevot.db.Data;
import ro.code4.monitorizarevot.db.Preferences;
import ro.code4.monitorizarevot.net.model.BranchDetails;
import ro.code4.monitorizarevot.net.model.Note;
import ro.code4.monitorizarevot.net.model.Section;
import ro.code4.monitorizarevot.net.model.QuestionAnswer;
import ro.code4.monitorizarevot.net.model.ResponseAnswerContainer;
import ro.code4.monitorizarevot.net.model.response.Ack;
import ro.code4.monitorizarevot.net.model.response.ResponseNote;
import ro.code4.monitorizarevot.net.model.response.VersionResponse;
import ro.code4.monitorizarevot.net.model.response.question.QuestionResponse;

public class NetworkService {

    private static ApiService mApiService;

    private static ApiService getApiService() {
        if (mApiService == null) {
            mApiService = initRetrofitInstanceWithUrl(BuildConfig.WEB_BASE_URL).create(ApiService.class);
        }
        return mApiService;
    }

    private static Retrofit initRetrofitInstanceWithUrl(String baseUrl) {
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().addInterceptor(interceptor);
        OkHttpClient client = clientBuilder.build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();
    }

    public static void doGetForm(String formId) throws IOException {
        Response<List<Section>> listResponse = getApiService().getForm(formId).execute();
        if(listResponse != null){
            if (listResponse.isSuccessful()) {
                Data.getInstance().saveFormDefinition(formId, listResponse.body());
            } else {
                throw new IOException(listResponse.message() + " " + listResponse.code());
            }
        } else {
            throw new IOException();
        }

    }

    public static VersionResponse doGetFormVersion() throws IOException {
        Response<VersionResponse> response = getApiService().getFormVersion().execute();
        if(response != null){
            if(response.isSuccessful()){
                return response.body();
            } else {
                throw new IOException(response.message() + " " + response.code());
            }
        } else {
            throw new IOException();
        }
    }

    public static Ack postBranchDetails(BranchDetails branchDetails) throws IOException {
        Response<Ack> response = getApiService().postBranchDetails(branchDetails).execute();
        if(response != null){
            if(response.isSuccessful()){
                return response.body();
            } else {
                throw new IOException(response.message() + " " + response.code());
            }
        } else {
            throw new IOException();
        }
    }

    public static QuestionResponse postQuestionAnswer(ResponseAnswerContainer responseMapper) throws IOException {
        if(responseMapper != null && responseMapper.getReponseMapperList().size()>0){
            Response<QuestionResponse> response = getApiService().postQuestionAnswer(responseMapper).execute();
            if(response != null){
                if(response.isSuccessful()){
                    for (QuestionAnswer questionAnswer : responseMapper.getReponseMapperList()) {
                        Data.getInstance().updateQuestionStatus(questionAnswer.getIdIntrebare());
                    }
                    return response.body();
                } else {
                    throw new IOException(response.message() + " " + response.code());
                }
            } else {
                throw new IOException();
            }
        } else {
            return null;
        }
    }

    public static ResponseNote postNote(Note note) throws IOException {
        MultipartBody.Part body = null;
        if (note.getUriPath() != null) {
            File file = new File(note.getUriPath());
            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        }
        Response<ResponseNote> response = getApiService().postNote(body,
                createMultipart("CodJudet", Preferences.getCountyCode()),
                createMultipart("NumarSectie", Preferences.getBranchNumber()),
                createMultipart("IdIntrebare", note.getQuestionId() != null ? note.getQuestionId() : 0),
                createMultipart("TextNota", note.getDescription())).execute();
        if (response != null) {
            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw new IOException();
            }
        } else {
            throw new IOException();
        }
    }

    private static MultipartBody.Part createMultipart(String name, int number) {
        return createMultipart(name, String.valueOf(number));
    }

    private static MultipartBody.Part createMultipart(String name, String value) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), value);
        return MultipartBody.Part.createFormData(name, null, requestBody);
    }
}