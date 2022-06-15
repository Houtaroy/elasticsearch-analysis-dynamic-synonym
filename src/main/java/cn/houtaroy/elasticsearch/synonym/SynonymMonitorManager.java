package cn.houtaroy.elasticsearch.synonym;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Houtaroy
 */
public final class SynonymMonitorManager {
  public static final Map<String, SynonymMonitor> monitors = new HashMap<>();
  private static final ScheduledExecutorService pool;

  static {
    pool = Executors.newScheduledThreadPool(1, SynonymMonitorManager::thread);
  }

  private SynonymMonitorManager() {
  }

  /**
   * enable synonym monitor
   *
   * @param synonym synonym
   * @param filter  filter
   */
  public static void enable(SynonymFile synonym, IDynamicSynonymFilter filter) {
    SynonymMonitor monitor = monitors.computeIfAbsent(
      synonym.getProperties().getName(),
      key -> new SynonymMonitor(key, synonym)
    );
    monitor.addFilter(filter);
    pool.scheduleWithFixedDelay(
      monitor,
      0,
      synonym.getProperties().getInterval(),
      java.util.concurrent.TimeUnit.SECONDS
    );
  }

  /**
   * create synonym thread
   * if runnable instanceof SynonymMonitor, thread name is "synonym-monitor-${synonym.name}"
   *
   * @param runnable runnable
   * @return thread
   */
  private static Thread thread(Runnable runnable) {
    Thread result = new Thread(runnable);
    if (runnable instanceof SynonymMonitor) {
      result.setName(String.format("synonym-monitor-%s", ((SynonymMonitor) runnable).getName()));
    }
    return result;
  }
}
