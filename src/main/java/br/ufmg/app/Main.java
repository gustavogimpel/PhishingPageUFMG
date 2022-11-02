package br.ufmg.app;

import br.ufmg.utils.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;


public class Main {
	public static void main(String[] args) {

		if(args.length > 1) {
			System.err.println("The only required parameter is the configuration filepath.");
			System.exit(-1);
		} else if (args.length < 1) {
			System.err.println("Required execution argument: path of the json configuration file.");
			System.exit(-1);
		}

		Path currentWorkDir = Paths.get("").toAbsolutePath();
		Path configFilePath = Paths.get(currentWorkDir.toString(), args[0]).toAbsolutePath();
		System.out.println("configFilePath: " + configFilePath.toString());


		int concurrentBrowserInstancesNumber = 0;
		int pageTimeout = 0;
		int windowTimeout = 0;
		int maxRequestNumber = 0;

		// Start reading configuration file
		try (FileReader reader = new FileReader(configFilePath.toString()))
		{
			JSONTokener jsonTokener = new JSONTokener(reader);
			JSONObject configObject = new JSONObject(jsonTokener);
			System.out.println(configObject);
			concurrentBrowserInstancesNumber = configObject.getInt("concurrentBrowsers");
			pageTimeout = configObject.getInt("pageTimeout");
			windowTimeout = configObject.getInt("windowTimeout");
			maxRequestNumber = configObject.getInt("maxRequests");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (JSONException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		Singleton.getInstance().setParameters(windowTimeout, maxRequestNumber);
		
		App aplicacao = new App(concurrentBrowserInstancesNumber,
								pageTimeout,
								maxRequestNumber);

		System.out.println("HERE");
		aplicacao.configurarCaminhos();
		aplicacao.obterArquivos();
		aplicacao.obterUrls();
		aplicacao.administrarProcessos();

	}
}
