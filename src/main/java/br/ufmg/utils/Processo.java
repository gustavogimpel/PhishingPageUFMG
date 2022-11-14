package br.ufmg.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.google.common.net.HttpHeaders;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.HarEntry;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.apache.commons.codec.digest.DigestUtils;

public class Processo implements Runnable {

	private int pid;
	private int timeout;
	private int limite_requisicoes;
	private BrowserMobProxy proxy;
	private Proxy seleniumProxy;
	private FirefoxDriver driver;
	private final BlockingQueue<String> listaUrls;
	private AtomicBoolean terminarProcessos;
	private AtomicBoolean reiniciarProcessos;
	private Map<String,Integer> dominiosBloqueados;
	private LogsWriter logsWriter;
	private URLList whitelist;
	private URLList blacklist;

	public Processo(BlockingQueue<String> listaUrls,AtomicBoolean terminarProcessos,AtomicBoolean reiniciarProcessos,int id, LogsWriter logsWriter, String diretorio, URLList whitelist,URLList blacklist,int timeout,int limite_requisicoes) {
		this.timeout = timeout;
		this.whitelist = whitelist;
		this.blacklist = blacklist;
		this.listaUrls = listaUrls;
		pid = id;
		this.logsWriter = logsWriter;
		dominiosBloqueados = new HashMap<String,Integer>();
		this.terminarProcessos = terminarProcessos;
		this.reiniciarProcessos = reiniciarProcessos;
		this.limite_requisicoes = limite_requisicoes;
	}

	public void getProxyServer() {
	     proxy = new BrowserMobProxyServer();

	     proxy.addRequestFilter((request, contents, messageInfo) -> {

	      String urlReq = io.netty.handler.codec.http.HttpHeaders.getHost(request);
		  String dom = "";
		  dom = urlReq.split(":")[0];

		  request.headers().set("X-Research-Project-Info","http://138.197.3.28/");

		  if (!dom.contains("firefox") && !dom.contains("mozilla") && !dom.contains("proxy")) {
			  long tempo = System.currentTimeMillis();
			  if (Singleton.getInstance().isInDict(dom)) {
				  int numRequisicoes = Singleton.getInstance().getNumeroReq(dom);
				  //System.out.println(dom + " " + numRequisicoes);
				  Singleton.getInstance().setNumeroReq(dom, tempo);
				  if (numRequisicoes >= limite_requisicoes && !whitelist.has(dom)) {
					  final HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(),HttpResponseStatus.valueOf(405));
					  response.headers().add(HttpHeaders.CONNECTION, "Close");
					  return response;
				  }
			  }else {
				  Singleton.getInstance().setNumeroReq(urlReq, tempo);
			  }
		  }

          if (request.getMethod().equals(HttpMethod.POST) || dom.contains(".gov") || blacklist.has(dom)) {
          	//System.out.println(request.headers());
          	final HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(),HttpResponseStatus.valueOf(405));
          	response.headers().add(HttpHeaders.CONNECTION, "Close");
          	return response;
          }else {
          	return null;
          }
	     });

	     // ---------------------------------------@---------------------------------
	     proxy.addLastHttpFilterFactory(new HttpFiltersSourceAdapter() {
	            @Override
	            public HttpFilters filterRequest(HttpRequest originalRequest) {
	                return new HttpFiltersAdapter(originalRequest) {
	                    @Override
	                    public HttpResponse proxyToServerRequest(HttpObject httpObject) {
	                        if (httpObject instanceof HttpRequest) {
	                        	((HttpRequest) httpObject).headers().remove("VIA");
	                            //System.out.println(((HttpRequest) httpObject).headers().get("VIA"));
	                        }
	                        return null;
	                    }
	                };
	            }
	        });

	     proxy.setTrustAllServers(true);
	     proxy.start();
	}

	public void getSeleniumProxy() {
		  seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
		  try {
			  String hostIp = Inet4Address.getLocalHost().getHostAddress();
			  seleniumProxy.setHttpProxy(hostIp+":" + proxy.getPort());
			  seleniumProxy.setSslProxy(hostIp+":" + proxy.getPort());
		  } catch (UnknownHostException e) {
		      e.printStackTrace();
		  }
	}

	public void getFirefoxDriver(DesiredCapabilities capabilities) {
		FirefoxOptions options = new FirefoxOptions();
		options.setProxy(seleniumProxy);
		options.setHeadless(true);
		options.merge(capabilities);

		System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"null");
		driver = new FirefoxDriver(options);
	}

	public Resposta acessaUrl(String urlComposta) {
		String[] temp = urlComposta.split("  ");
		String url = temp[0];
		//System.out.println(url);

		String dom = "";
		if (url.contains("http") == true) {
			dom = url.split("/")[2];
		}else {
			dom = url.split("/")[0];
		}

		if ( dominiosBloqueados.get(dom) != null  && dominiosBloqueados.get(dom) >= 10) {
			String out = urlComposta.replace("\n","")+"  BLOQUEADO  0\n";
			Resposta resposta = new Resposta(true,false,out);
			return resposta;
		}

        proxy.newHar("url_"+Integer.toString(pid));
        driver.manage().timeouts().pageLoadTimeout(this.timeout, TimeUnit.SECONDS);
        String finalUrl = "about:blank";
        try {
        	driver.get(url);
        	finalUrl = driver.getCurrentUrl();
        }catch(Exception e) {
        	if (e instanceof WebDriverException ) {
        		if ( dominiosBloqueados.get(dom) == null ) {
    				dominiosBloqueados.put(dom, 1);
            	}else {
            		int valor = dominiosBloqueados.get(dom);
            		valor += 1;
            		dominiosBloqueados.replace(dom, valor);
            	 }
        	}
        	String urlBroken = urlComposta;

        	try {
				logsWriter.writeFirefoxException(this.pid, urlBroken + e.toString());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

        	String nomeExc = e.getClass().getSimpleName();
        	String out = urlComposta.replace("\n","")+"  "+nomeExc+"  0\n";
			Resposta resposta = new Resposta(true,false,out);
			return resposta;
        }

        if (finalUrl != "about:blank") {

        	InetAddress ip = null;
        	String ipTexto = null;
        	try {
				String hostname = new URL(finalUrl).getHost();
				ip = InetAddress.getByName(hostname);
			 	ipTexto = ip.getHostAddress();
			} catch (MalformedURLException e) {
				//e.printStackTrace();
				finalUrl = "-";
				ipTexto = "0";
			} catch (UnknownHostException e) {
				//e.printStackTrace();
				if ( dominiosBloqueados.get(dom) == null ) {
    				dominiosBloqueados.put(dom, 1);
            	}else {
            		int valor = dominiosBloqueados.get(dom);
            		valor += 1;
            		dominiosBloqueados.replace(dom, valor);
            	}
				finalUrl = "-";
				ipTexto = "0";
			}

			String out = urlComposta.replace("\n","")+"  "+finalUrl+"  "+ ipTexto +"\n";
			// TODO: tem uma exceção sendo lançada abaixo: (consertada , talvez)
			String hash;
        	String page;
			try {
        		String html = driver.getPageSource();
        		Document document = Jsoup.parse(html);
            	page = document.toString();

            	hash = DigestUtils.md5Hex(page);
        	} catch (Exception e) {
        		page = "";
        		hash = "EMPTYPAGE";
        	}

			String url8 = out.replace("\n","") + "  " + hash.toString() + "\n";

			try {
				this.logsWriter.writeSourcePage(this.pid, url8);
				this.logsWriter.writeSourcePage(this.pid, page);
				this.logsWriter.writeSourcePage(this.pid, "\n*!-@x!x@-!*\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        	//System.out.println("finished");
        	return new Resposta(false,false,out,proxy.getHar().getLog().getEntries());

        }
        return new Resposta(true,false,"wtf");
	}

	public void run() {
		System.setProperty("webdriver.gecko.driver", "/home/vrjuliao/.local/bin/geckodriver");
		DesiredCapabilities capabilities = new DesiredCapabilities();

		getProxyServer();
		getSeleniumProxy();
		capabilities.setCapability("marionette", true);
		getFirefoxDriver(capabilities);

		// abrirArquivos();

		while (terminarProcessos.get() == false) {
			try {
				if (reiniciarProcessos.get()) {
					break;
				}
				double tempoInicio = System.currentTimeMillis();
				String urlComposta = listaUrls.take();

				if (urlComposta == "poison_pill") {
					terminarProcessos.compareAndSet(false,true);
					break;
				}

				Resposta resposta = acessaUrl(urlComposta);
				String urlLog = resposta.getUrlLog();
				//System.out.println(urlLog);
				this.logsWriter.writeAccessLog(this.pid, urlLog);
				this.logsWriter.writeTcp(this.pid, urlLog.replace("\n",""));
				this.logsWriter.writeCadeiaURLs(this.pid, urlLog);

	            Set<String> conjuntoIps = new HashSet<String>();
	            if(resposta.getExcecao() == false  && resposta.getBloqueado() != true) {
	            	List<HarEntry> entries = resposta.getHar();
	            	for(HarEntry entry : entries) {
	            		String ip = entry.getServerIPAddress();
	            		int statusCode = entry.getResponse().getStatus();
	            		//System.out.println(statusCode);
	            		conjuntoIps.add(ip);
	            		String urlInicial = entry.getRequest().getUrl();
	            		String urlFinal = entry.getResponse().getRedirectURL();

	            		if (!urlFinal.contains("mozilla") && !urlInicial.contains("mozilla")
	            				&& !urlFinal.contains("firefox") && !urlInicial.contains("firefox")) {
	            			if (urlFinal != "") {
		            			String timeStamp = entry.getStartedDateTime().toString();
								this.logsWriter.writeCadeiaURLs(this.pid, timeStamp.replace(" ","") + "  " + urlInicial + "  " + urlFinal + "  " + statusCode);
		            		}else {
		            			if (urlInicial != "") {
		            				String timeStamp = entry.getStartedDateTime().toString();
									this.logsWriter.writeCadeiaURLs(this.pid, timeStamp.replace(" ","") + "  " + urlInicial + "  -"+statusCode+"\n");
		            			}
		            		}
	            		}
	            	}
	            	String cadeiaIps = String.join(",", conjuntoIps);

					this.logsWriter.writeTcp(this.pid, "  "+cadeiaIps);
	            }

				this.logsWriter.writeTcp(this.pid, "\n*!-@x!x@-!*\n");
				this.logsWriter.writeCadeiaURLs(this.pid, "*!-@x!x@-!*\n");
				double tempoFinal = System.currentTimeMillis();
				this.logsWriter.writeTimeURLs(this.pid, Double.toString((tempoFinal-tempoInicio)/1000.0)+"\n");

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			driver.close();
			proxy.stop();
		}catch(Exception e) {

		}

		terminate();
	}

	public void terminate() {
		try {
			System.out.println("Processo "+pid+" terminado.");
			logsWriter.closeFiles();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
