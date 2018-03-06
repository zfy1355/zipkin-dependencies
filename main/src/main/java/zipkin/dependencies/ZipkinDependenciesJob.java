/**
 * Copyright 2016-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.dependencies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin.dependencies.cassandra.CassandraDependenciesJob;
import zipkin.dependencies.elasticsearch.ElasticsearchDependenciesJob;
import zipkin.dependencies.mysql.MySQLDependenciesJob;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ZipkinDependenciesJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinDependenciesJob.class);
  private static final int ONE_DAY = 24*60*60;
  private static final int ONE_MINUTE = 60;
  /** Runs with defaults, starting today */
  public static void main(String[] args) throws UnsupportedEncodingException {
    String jarPath = pathToUberJar();
    long day = args.length == 1 ? parseDay(args[0]) : System.currentTimeMillis();
    String storageType = System.getenv("STORAGE_TYPE");
    if (storageType == null) {
      throw new IllegalArgumentException("STORAGE_TYPE not set");
    }

    String zipkinLogLevel = System.getenv("ZIPKIN_LOG_LEVEL");
    if (zipkinLogLevel == null) zipkinLogLevel = "INFO";
    Runnable logInitializer = LogInitializer.create(zipkinLogLevel);
    logInitializer.run(); // Ensures local log commands emit

    Runnable runnable =  new Runnable() {
      public void run() {
        try {
          switch (storageType) {
            case "cassandra":
              CassandraDependenciesJob.builder()
                      .logInitializer(logInitializer)
                      .jars(jarPath)
                      .day(day)
                      .build()
                      .run();
              break;
            case "cassandra3":
              zipkin.dependencies.cassandra3.CassandraDependenciesJob.builder()
                      .logInitializer(logInitializer)
                      .jars(jarPath)
                      .day(day)
                      .build()
                      .run();
              break;
            case "mysql":
              MySQLDependenciesJob.builder()
                      .logInitializer(logInitializer)
                      .jars(jarPath)
                      .day(day)
                      .build()
                      .run();
              break;
            case "elasticsearch":
              ElasticsearchDependenciesJob.builder()
                      .logInitializer(logInitializer)
                      .jars(jarPath)
                      .day(day)
                      .build()
                      .run();
              LOGGER.info("elasticSearch job run time:" + Calendar.getInstance().getTime());
              System.out.println("elasticSearch job run time:" + Calendar.getInstance().getTime());
              break;
            default:
              throw new UnsupportedOperationException("Unsupported STORAGE_TYPE: " + storageType);
          }
        }catch (Exception e){
          if(e instanceof UnsupportedOperationException)
            throw e;
          else
            LOGGER.error(e.getMessage());
        }
      }
    };
    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    String rate = System.getenv("JOB_RATE");
    if(rate == null){
      service.scheduleAtFixedRate(runnable
              , 0, ONE_MINUTE * 5, TimeUnit.SECONDS);
    }else{
      service.scheduleAtFixedRate(runnable
              , 0, Integer.parseInt(rate), TimeUnit.SECONDS);
    }
  }

  static String pathToUberJar() throws UnsupportedEncodingException {
    URL jarFile = ZipkinDependenciesJob.class.getProtectionDomain().getCodeSource().getLocation();
    return URLDecoder.decode(jarFile.getPath(), "UTF-8");
  }

  static long parseDay(String formattedDate) {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    try {
      return df.parse(formattedDate).getTime();
    } catch (ParseException e) {
      throw new IllegalArgumentException(
              "First argument must be a yyyy-MM-dd formatted date. Ex. 2016-07-16");
    }
  }
}
