# Elasticsearch 动态同义词插件

[English Documentation](https://github.com/Houtaroy/elasticsearch-analysis-dynamic-synonym/tree/main/docs/en)

本项目fork自[bells/elasticsearch-analysis-dynamic-synonym](https://github.com/bells/elasticsearch-analysis-dynamic-synonym)

重构部分代码, 并支持以下的同步类型:

- 文件: `file`
- 远程: `remote`
- JDBC: `jdbc`

## 版本对照

| 插件版本   | ES版本          |
|--------|---------------|
| master | 7.x -> master |
| 6.1.4  | 6.1.4         |
| 5.2.0  | 5.2.0         |
| 5.1.1  | 5.1.1         |
| 2.3.0  | 2.3.0         |
| 2.2.0  | 2.2.0         |
| 2.1.0  | 2.1.0         |
| 2.0.0  | 2.0.0         |
| 1.6.0  | 1.6.X         |

## 安装

### 下载安装

```bash
./bin/elasticsearch-plugin install https://github.com/Houtaroy/elasticsearch-analysis-dynamic-synonym/releases/download/${版本号}/elasticsearch-analysis-dynamic-synonym-${版本号}.zip
```

### 编译安装

1. 克隆或下载本仓库

2. `mvn package`

3. 将压缩文件 `target/releases/elasticsearch-analysis-dynamic-synonym-${版本号}.zip`的内容复制到`${ES目录}/plugins/analysis-dynamic-synonym`

## 示例

```json
{
  "index": {
    "analysis": {
      "analyzer": {
        "synonym": {
          "tokenizer": "whitespace",
          "filter": [
            "remote_synonym"
          ]
        }
      },
      "filter": {
        "file_synonym": {
          "type": "dynamic_synonym",
          "synonym_type": "file",
          "uri": "/dynamic_synonym/synonym.txt"
        },
        "remote_synonym": {
          "type": "dynamic_synonym",
          "synonym_type": "remote",
          "uri": "https://localhost:8080/api/synonyms",
          "interval": 30
        },
        "jdbc_graph": {
          "type": "dynamic_synonym_graph",
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

| 名称 | 同步类型 | 必填 | 默认值                       | 描述                                            |
|----------------| ------ | :------: | --------------------------------------- | ------------------------------------------------------------ |
| type           | all |    √     |                                         | `dynamic_synonym`/`dynamic_synonym_graph`                    |
| synonym_type   | all    |    √     |                                         | `file`/`remote`/`jdbc`                                       |
| uri            | all    |    √     |                                         | 同义词文件URI                  |
| interval       | all    |    ×     | 60(同步类型是`jdbc`为3600)               | 同义词刷新时间间隔, 单位秒 |
| expand         | all    |    ×     | true                                    | expand                                                       |
| lenient        | all    |    ×     | false                                   | lenient on exception thrown when importing a synonym         |
| format         | all    |    ×     | ' '                                     | synonym file format, for WordNet structure this can be set to `wordnet` |
| driver_class_name | `jdbc` |    √     |                                         | JDBC驱动类全路径名称        |
| username       | `jdbc` |    √     |                                         | JDBC连接用户名                         |
| password       | `jdbc` |    √     |                                         | JDBC连接密码                                                 |
| synonym_sql    | `jdbc` |    ×     | `select synonym from t_synonym`         | 同义词查询语句, 返回结果为字符串列表 |
| version_sql    | `jdbc` |    ×     | `select version from t_synonym_version` | 同义词版本查询语句, 返回结果为整形 |

**当同步类型为`jdbc`时, 请将对应的JDBC驱动jar包复制到`${ES目录}/plugins/analysis-dynamic-synonym`**

## 刷新机制

### file

依据文件修改时间

### remote

获取请求头中的`Last-Modified`或`ETag`, 如果发生改变则刷新

### jdbc

1. 执行同义词版本查询语句, 取第一行第一列结果与内存记录的版本进行比较
2. 如果数据库中的版本较大, 执行同义词查询语句, 将所有行的第一列作为同义词结果进行刷新

注意: 文件编码应为`utf-8`
