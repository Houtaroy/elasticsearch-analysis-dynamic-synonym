/**
 *
 */
package cn.houtaroy.elasticsearch.synonym;

import org.apache.lucene.analysis.synonym.SynonymMap;

import java.io.Reader;

/**
 * @author bellszhu
 */
public interface SynonymFile {
  SynonymProperties getProperties();

  SynonymMap reloadSynonymMap();

  boolean isNeedReloadSynonymMap();

  Reader getReader();

}