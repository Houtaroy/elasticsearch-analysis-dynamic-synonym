# Dynamic Synonym for ElasticSearch

The dynamic synonym plugin adds a synonym token filter that reloads the synonym file at given intervals

Synonym type: `file`/`remote`/`jdbc`

## Version

dynamic synonym version | ES version
-----------|-----------
master| 7.x -> master
6.1.4 |	6.1.4
5.2.0 |	5.2.0
5.1.1 |	5.1.1
2.3.0 | 2.3.0
2.2.0 | 2.2.0
2.1.0 | 2.1.0
2.0.0 | 2.0.0 
1.6.0 | 1.6.X

## Installation

### download

```bash
./bin/elasticsearch-plugin install https://github.com/Houtaroy/elasticsearch-analysis-dynamic-synonym/releases/download/${version}/elasticsearch-analysis-dynamic-synonym-${version}.zip
```

### compile

1. clone or download repository

2. `mvn package`

3. copy and unzip `target/releases/elasticsearch-analysis-dynamic-synonym-${version}.zip` to `${ES_HOME}/plugins/analysis-dynamic-synonym`

## Example

```json
{
    "index" : {
        "analysis" : {
            "analyzer" : {
                "synonym" : {
                    "tokenizer" : "whitespace",
                    "filter" : ["remote_synonym"]
                }
            },
            "filter" : {
                "file_synonym" : {
                    "type" : "dynamic_synonym",
                    "synonym_type": "file",
                    "uri" : "/dynamic_synonym/synonym.txt",
                },
                "remote_synonym" : {
                    "type" : "dynamic_synonym",
                    "synonym_type": "remote",
                    "uri" : "https://localhost:8080/api/synonyms",
                    "interval": 30
                },
                "jdbc_graph" : {
                    "type" : "dynamic_synonym_graph",
                    "synonym_type": "jdbc",
                    "uri": "jdbc:mysql://localhost:3306/elasticsearch?allowPublicKeyRetrieval=true",
                    "driver_class_name": "com.mysql.cj.jdbc.Driver",
                    "username": "elasticsearch",
                    "password": "elasticsearch"
                }
            }
        }
    }
}
```
## Configuration

| name              | type   | required | default                                 | description                                                  |
| ----------------- | ------ | :------: | --------------------------------------- | ------------------------------------------------------------ |
| type              | all    |    √     |                                         | `dynamic_synonym`/`dynamic_synonym_graph`                    |
| synonym_type      | all    |    √     |                                         | `file`/`remote`/`jdbc`                                       |
| uri               | all    |    √     |                                         | synonyms URI, file path name or URL                          |
| interval          | all    |    ×     | 60(`jdbc` is 3600)                      | refresh interval in seconds for the synonym                  |
| expand            | all    |    ×     | true                                    | expand                                                       |
| lenient           | all    |    ×     | false                                   | lenient on exception thrown when importing a synonym         |
| format            | all    |    ×     | ' '                                     | synonym file format, for WordNet structure this can be set to `wordnet` |
| driver_class_name | `jdbc` |    √     |                                         | jdbc driver class name                                       |
| username          | `jdbc` |    √     |                                         | jdbc connect username                                        |
| password          | `jdbc` |    √     |                                         | jdbc connect password                                        |
| synonym_sql       | `jdbc` |    ×     | `select synonym from t_synonym`         | synonym select SQL, result must be string                    |
| version_sql       | `jdbc` |    ×     | `select version from t_synonym_version` | synonym version select SQL, result must be int               |

**For `jdbc` type, you should copy your jdbc driver jar to `${ES_HOME}/plugins/analysis-dynamic-synonym`**

## Update mechanism

### file

Determined by modification time of the file, if it has changed the synonyms wil

### remote

Reads out the `Last-Modified` and `ETag` http header. If one of these changes, the synonyms will be reloaded. 

### jdbc

1. execute `version_sql`, get first column to compare
2. if persistence version is larger, execute `synonym_sql`, get first column to update synonyms

**Note:** File encoding should be an utf-8 text file. 
