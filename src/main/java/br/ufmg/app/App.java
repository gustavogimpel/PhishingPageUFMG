package br.ufmg.app;

import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import java.util.concurrent.*;
import java.util.function.Predicate;

import br.ufmg.utils.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class App {

	private Configuration config;
	private LogsWriter logsWriter;
	private URLList whiteList;
	private URLList blackList;
	private File[] arquivos;
	private BlockingQueue<String> listaUrls;
	private AtomicBoolean reiniciarProcessos;
	private AtomicBoolean terminarProcessos;

	/* Inicialização de variáveis.*/
	public App(Configuration config) { //int instancias, int timeout, int limite_requisicoes, Path repository, Path whiteList, Path blackList, Path logsDir) {
		try {
			this.whiteList = new URLList(config.getWhiteListPath());
			this.blackList = new URLList(config.getBlackListPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.config = config;
		listaUrls = new LinkedBlockingDeque<String>();
		reiniciarProcessos =  new AtomicBoolean();
		reiniciarProcessos.set(false);
		terminarProcessos =  new AtomicBoolean();
		terminarProcessos.set(false);
	}

	public void startLogFiles() throws FileNotFoundException, UnsupportedEncodingException, IOException {
		this.logsWriter = new LogsWriter(config.getLogsDirPath(), config.getConcurrentBrowserInstancesNumber());
		this.logsWriter.createFiles();
	}

	/* Função que realiza a leitura de arquivos. */
	public void obterArquivos() {

		File repo = this.config.getRepositoryPath().toFile();
		if ( repo.isDirectory() ) {
			arquivos = repo.listFiles();
			Arrays.sort(arquivos, Comparator.comparingLong(File::lastModified));
		} else {
			try {
				String data = "Recipiente de urls inexistente: "+ this.logsWriter.getFormatedDate();
				Files.write(Paths.get(this.logsWriter.getStandardFileNameFromSuffix("recip")), data.getBytes());
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if (arquivos.length == 0) {
			System.exit(0);
		}
	}

	/* Função que realiza a leitura de URLs. */
	public void obterUrls() {
		Charset charset = Charset.forName("UTF-8");
		for (File arquivo : arquivos) {
			try {
				List<String> linhas = Files.readAllLines(arquivo.toPath(),charset);
				for (String linha : linhas) {
					listaUrls.add(linha);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
		/*for (int i = 0;i < this.instancias;i++) {
			listaUrls.add("poison_pill");
		}*/
		System.out.println(listaUrls.size());
	}

	/* Função que determina se a aplicação deve parar, realizando
	 * a leitura de um arquivo na pasta shellscripts/sys/operante. */
	public int appOperante() {
		int status = 0;
		int tries = 0;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/home/vrjuliao/workfolder/web-phishing-framework/data/operante"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		String line = null;
		while(tries < 3) {
			try {
				while ((line = br.readLine()) != null) {
				  String[] parts = line.split(" ");
				  status = Integer.parseInt(parts[0]);
				  //System.out.println(status);
				}
				break;
			} catch (IOException e) {
				tries++;
			}
		}
		return status;
	}

	/* Função principal. Administa o multithreading */
	@SuppressWarnings("deprecation")
	public void administrarProcessos() {
		SimpleDateFormat dataInicio = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");

		Date data = new Date();
		String dataFormatada = dataInicio.format(data);
		String inicio = "Inicio em "+dataFormatada+"\n";
		try {
			Files.write(Paths.get(this.config.getLogsDirPath().toString(), this.logsWriter.getStandardFileNameFromSuffix("inicio")), inicio.getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		MonitorMemoria memoryMonitor = new MonitorMemoria(reiniciarProcessos);
		Thread monitor = new Thread(memoryMonitor);
		monitor.start();

		List<Thread> listaThreads = new LinkedList<Thread>();

		Predicate<Thread> isDead = t -> !t.isAlive();

		long tempoInicio = System.nanoTime();
		int indice = 0;

		while(appOperante() == 1) {

			if(terminarProcessos.get()) {
				break;
			}

			if(reiniciarProcessos.get()) {
				//terminarProcessos.set(true);
				System.out.println("Esperando");
				for (Thread thread : listaThreads ) {
					try {
						thread.join(600000);
					} catch (InterruptedException e) {
						continue;
					}
				}
				Process pr;
				try {
					pr = Runtime.getRuntime().exec("pkill -9 firefox");
					pr = Runtime.getRuntime().exec("pkill -9 geckodriver");
				} catch (IOException e) {
					e.printStackTrace();
				}
				//terminarProcessos.set(false);
				reiniciarProcessos.set(false);
			}
			listaThreads.removeIf(isDead);
			if(listaThreads.size() >= this.config.getConcurrentBrowserInstancesNumber()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else {
				Processo r = new Processo(listaUrls, terminarProcessos, reiniciarProcessos,
							 indice, this.logsWriter, this.config.getLogsDirPath().toString()+"/",
							  whiteList, blackList, this.config.getPageTimeout(),
							  this.config.getMaxRequestNumber());
				Thread t = new Thread(r);
				listaThreads.add(t);
				t.start();
				System.out.println("Thread "+Integer.toString(indice)+" criada");
				indice += 1;
			}
		}

		for (Thread thread : listaThreads ) {
			try {
				thread.join(600000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		monitor.interrupt();
		escreverUrlsRestantes();
		System.out.println("aaaa");
		System.gc();

		long tempoFinal = System.nanoTime();
		long tempoDecorrido = tempoFinal - tempoInicio;
		String tempoString = Long.toString(tempoDecorrido) + '\n';

		try {
			Files.write(Paths.get(this.config.getLogsDirPath().toString(), this.logsWriter.getStandardFileNameFromSuffix("time")), tempoString.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void escreverUrlsRestantes() {
		FileWriter restantes = null;
		try {
			restantes = new FileWriter(this.config.getRepositoryPath().resolve("urlsrestantes"), false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		while (listaUrls.isEmpty() == false) {
			try {
				String url = listaUrls.take();
				restantes.write(url+"\n");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			restantes.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
