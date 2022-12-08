import psutil
import os
from pathlib import Path
import json
from datetime import datetime
import subprocess
import time

def get_configuration():
  return {
    # 'concurrentBrowsers': ,
    # 'pageTimeout': ,
    # 'logsDirPath': './logs-dir'
    'windowTimeout': 1800,
    'maxRequests': 60,
    'repositoryPath': '../../example/repo',
    'geckodriverBinPath': '/usr/bin/geckodriver',
    'runtimeControllersPath': '../runtime_params'
  }


def main():
  # get the path of the current file
  current_filepath = Path(__file__).parent.resolve().absolute()
  runtime_params_dir =  current_filepath.joinpath("runtime_params")
  os.mkdir(runtime_params_dir)

  # create log file
  benchmarks_log_file = open(current_filepath.joinpath('out.log'), 'w')

  # for concurrent_browsers_number in range(1, 9): # threads number starts from 1 and goes to 8.
  for concurrent_browsers_number in range(2, 9): # threads number starts from 1 and goes to 8.
    for page_timeout in range(10, 61, 10): # starts from 10 seconds to 60 seconds by incrementing 10 seconds on each iteration.
      # create runtime environment
      with open(runtime_params_dir.joinpath("running"), 'w', encoding='utf-8') as is_running_file:
        print(1, file=is_running_file)

      logs_dir =  current_filepath.joinpath("{}-{}".format(concurrent_browsers_number, page_timeout))
      os.mkdir(logs_dir)

      # write variable parameters
      execution_config = get_configuration()
      execution_config['concurrentBrowsers'] = concurrent_browsers_number
      execution_config['pageTimeout'] = page_timeout
      execution_config['logsDirPath'] = str(logs_dir)

      config_file_path = logs_dir.joinpath('config.json')
      with open(config_file_path, 'w') as json_config_file:
        json.dump(execution_config, json_config_file)
      start_time = datetime.now()

      print("{}-{}".format(concurrent_browsers_number, page_timeout), file=benchmarks_log_file)
      # web_phishing_framework = subprocess.Popen(['java', '-jar', '../target/WebPhishingFramework.jar', str(config_file_path)],
      web_phishing_framework = psutil.Popen(['java', '-jar', '../target/WebPhishingFramework.jar', str(config_file_path)],
                                      cwd=str(current_filepath), stdout=benchmarks_log_file, stderr=benchmarks_log_file, text=True)

      running_time = datetime.now() - start_time
      # process = psutil.Process(web_phishing_framework.pid)
      # while(running_time.total_seconds() < (2*60*60)): # while the running time is less than two hours
      while(running_time.total_seconds() < 60): # while the running time is less than two hours
        if(web_phishing_framework.poll() is not None):
          break

        # collect process data
        # write collected data to a file
        with web_phishing_framework.oneshot():
          print(web_phishing_framework.cpu_percent(interval=0.1)/psutil.cpu_count())
          print(web_phishing_framework.memory_percent(memtype="rss"))

        # sleep for 30 seconds
        time.sleep(30)
        running_time = datetime.now() - start_time

      if(web_phishing_framework.poll() is not None):
        print("========= ERROR ABOVE ========", file=benchmarks_log_file)

      with open(runtime_params_dir.joinpath("running"), 'w', encoding='utf-8') as is_running_file:
        print(0, file=is_running_file)

      web_phishing_framework.wait()

      # get the quantity of not-computed URLS
      # write to the output file the quantity of computed and not-computed URLS

  benchmarks_log_file.close()

  # para cada combinação de configuração, rodar por 2 horas:
    # a cada 30 segundos, pegar as informações necessárias dos processos.
    # criar alguns arquivos para escrever os resultados
    # escrever os resultados


if (__name__ == '__main__'):
  main()