package com.fmat.proyecto3.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Intent;

import com.fmat.proyecto3.json.ExerciseAnswer;
import com.fmat.proyecto3.json.ExerciseFactory;
import com.fmat.proyecto3.json.ServerMessage;
import com.google.gson.Gson;

/**
 * Servicio de env�o de resoluciones de ejercicios
 * @author Irving Caro
 *
 */
public class ExercisePostService extends ExerciseRESTService {

	private static final String TAG = ExercisePostService.class.getName();

	/**
	 * Acci�n del servicio
	 */
	public static final String INTENT_RESULT_ACTION = "com.fmat.REST_POST_RESULT";

	/**
	 * C�digo de respuesta del servidor para error de duplicado de env�o
	 */
	public static final int EXERCISE_ALREADY_SOLVED_BY_USER_SERVER_RESPONSE_CODE = 409;
	/**
	 * C�digo para el resultado de la acci�n
	 */
	public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
	/**
	 * C�digo para la respuesta del servicio
	 */
	public static final String EXTRA_EXERCISE_ANSWER = "EXTRA_EXERCISE_ANSWER";

	/**
	 * Constructor
	 */
	public ExercisePostService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// When an intent is received by this Service, this method
		// is called on a new thread.

		super.onHandleIntent(intent);

		if (!super.hasExtras())
			return;

		ExerciseAnswer answer = (ExerciseAnswer) extras
				.get(EXTRA_EXERCISE_ANSWER);

		String message = null;
		Intent resultIntent = new Intent(INTENT_RESULT_ACTION);

		try {
			// Here we define our base request object which we will
			// send to our REST service via HttpClient.
			HttpRequestBase request = new HttpPost();
			request.setURI(new URI(url.toString()));

			HttpClient client = new DefaultHttpClient();

			// Attach form entity if necessary. Note: some REST APIs
			// require you to POST JSON. This is easy to do, simply use
			// postRequest.setHeader('Content-Type', 'application/json')
			// and StringEntity instead. Same thing for the PUT case
			// below.

			HttpPost postRequest = (HttpPost) request;
			postRequest.setHeader("Content-Type", "application/json");

			StringEntity attachedEntity = ExerciseFactory
					.marshallAnswer(answer);

			postRequest.setEntity(attachedEntity);

			// Finally, we send our request using HTTP. This is the
			// synchronous
			// long operation that we need to run on this thread.
			HttpResponse response = client.execute(request);
			// HttpEntity responseEntity = response.getEntity();

			HttpEntity responseEntity = response.getEntity();

			if (responseEntity != null) {

				InputStreamReader contentReader = new InputStreamReader(
						responseEntity.getContent());

				ServerMessage serverMessage = new Gson().fromJson(contentReader, ServerMessage.class);

				resultIntent.putExtra(ServerMessage.EXTRA_SERVER_MESSAGE, serverMessage.isCorrect());

			}
			
			
			int statusCode = response.getStatusLine().getStatusCode();

			resultIntent.putExtra(EXTRA_RESULT_CODE, statusCode);

		} catch (UnsupportedEncodingException e) {
			message = "La direccion del servicio es invalida.";
			// Log.e(TAG, errorMessage, e);
		} catch (URISyntaxException e) {
			message = "La direccion del servicio es invalida.";
		} catch (ClientProtocolException e) {
			message = "Hubo un problema al contactar al servidor.";
			// Log.e(TAG, errorMessage, e);
		} catch (ConnectTimeoutException e) {
			message = "No hay conexion a Internet.";
			// Log.e(TAG, errorMessage, e);
		} catch (IOException e) {
			message = "Hubo un problema al contactar al servidor.";
			// Log.e(TAG, errorMessage, e);
		}

		if (message != null)
			resultIntent.putExtra(EXTRA_ERROR_MESSAGE, message);

		sendBroadcast(resultIntent);

	}
}
