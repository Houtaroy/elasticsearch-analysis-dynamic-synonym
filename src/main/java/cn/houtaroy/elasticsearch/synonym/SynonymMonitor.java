package cn.houtaroy.elasticsearch.synonym;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Houtaroy
 */
@Getter
@RequiredArgsConstructor
public class SynonymMonitor implements Runnable {
  private static final Logger logger = LogManager.getLogger("dynamic-synonym");
  protected final String name;
  protected final SynonymFile synonym;
  protected int version;
  protected Map<Class<? extends IDynamicSynonymFilter>, IDynamicSynonymFilter> filters = new HashMap<>();

  /**
   * add synonym filter
   *
   * @param filter synonym filter
   */
  public void addFilter(IDynamicSynonymFilter filter) {
    filters.putIfAbsent(filter.getClass(), filter);
  }

  @Override
  public void run() {
    if (synonym.isNeedReloadSynonymMap()) {
      for (Class<? extends IDynamicSynonymFilter> key : filters.keySet()) {
        filters.get(key).update(synonym.reloadSynonymMap());
        logger.info("success reload synonym");
      }
    }
  }
}
