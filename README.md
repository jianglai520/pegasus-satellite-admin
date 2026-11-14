# 飞马星球卫星微管理系统

## 项目简介

本项目是面向对象技术（Java）大作业一，使用字符界面模拟“主机 + 终端”的卫星管理程序。系统以飞马星球卫星管理局为业务背景，使用 Java 面向对象技术实现卫星的注册、查询、修改、激活、封锁、删除归档和修改历史管理。

项目采用抽象类、接口、继承、封装和 `ArrayList` 组织数据，所有业务规则集中在领域类与管理类中，界面层只负责输入输出。

## 功能

程序主菜单提供以下功能：

1. 显示当前卫星列表
2. 注册添加新卫星
3. 查询卫星详细信息（按 COSPARID）
4. 修改卫星名称
5. 搜索卫星（按 ID 或名称，支持模糊匹配）
6. 封锁卫星（封锁后不得修改信息和删除）
7. 激活卫星（重新启用）
8. 显示失活卫星列表（已封锁 / 已归档）
9. 查看和修改卫星功能与注意事项
10. 查看卫星注册时间与历次修改记录
11. 删除卫星并归档
0. 退出系统

## 业务规则

- 最低卫星轨道半径为 1.2；
- 任意两颗卫星的轨道间距不得小于 0.2；
- COSPARID 共 6 位，由 2 位国家编码和 4 位数字组成，全局唯一，分配后不可修改；
- 卫星状态分为 `ACTIVATED`、`DEACTIVATED`、`DELETE`；
- 封锁状态的卫星仍占用原轨道，且不得修改信息和删除；
- 卫星报废后从在用列表移除、释放轨道，并归档保存，状态标记为 `DELETE`；
- 已归档的 COSPARID 永久保留，不得再次使用；
- 注册时间和每次修改时间由系统自动生成，不允许人工修改；
- 程序启动时预加载 5 颗卫星，`ArrayList` 初始容量为 10；
- 主菜单使用 `do-while` 循环，选择 0 时通过 `System.exit(0)` 退出。

## 主要设计

- `Satellite`：抽象父类，封装 COSPARID、名称、轨道半径、状态、功能与注意事项、注册时间、修改时间和修改历史，并实现改名、功能维护、状态迁移等公共行为。
- `ChinaSatellite`、`UsSatellite`、`RussiaSatellite`：国家卫星子类，分别扩展任务类型、运营性质、设计寿命，并实现国家编码、国家名称、所属机构和扩展信息等抽象方法。
- `Activatable`：可激活接口，声明 `activate()`、`deactivate()`、`isActivated()`。
- `Archivable`：可归档接口，声明 `archive()`、`isArchived()`、`getArchivedAt()`。
- `SatelliteManager`：业务管理类，维护当前卫星和归档卫星两个 `ArrayList`，集中实现注册校验、编号唯一、轨道冲突、名称唯一、查询、激活、封锁和归档等规则。
- `ConsoleUI`：字符界面工具类，负责输入校验、表格对齐、详情和历史记录排版。
- `Main`：程序入口，负责菜单循环和功能分派。
- `ModificationRecord`：不可变修改记录；`SatelliteStatus`：状态枚举；`BusinessException`：业务异常。

## 目录结构

```text
FM_320240931231_令小刚/
├── src/
│   ├── Main.java
│   ├── ConsoleUI.java
│   ├── Satellite.java
│   ├── ChinaSatellite.java
│   ├── UsSatellite.java
│   ├── RussiaSatellite.java
│   ├── SatelliteManager.java
│   ├── Activatable.java
│   ├── Archivable.java
│   ├── SatelliteStatus.java
│   ├── ModificationRecord.java
│   └── BusinessException.java
├── FM_320240931231_令小刚_作业报告.docx
└── README.md
```

## 预加载卫星

| COSPARID | 名称 | 轨道半径 | 国家 | 扩展信息 |
|---|---|---|---|---|
| CN0001 | 飞马号-0001 | 1.20 | 中国 | 任务类型：通信 |
| CN0002 | 飞马号-0002 | 1.40 | 中国 | 任务类型：遥感 |
| US0001 | PEGASUS-0001 | 1.70 | 美国 | 运营性质：政府 |
| US0002 | PEGASUS-0002 | 2.00 | 美国 | 运营性质：商业 |
| RU0001 | SAT-0001 | 2.30 | 俄罗斯 | 设计寿命：8 年 |

## 编译与运行

运行环境：华为云鲲鹏弹性云服务器，openEuler 22.03 LTS，JDK 8。

```bash
cd FM_320240931231_令小刚
javac -encoding UTF-8 -d out src/*.java
java -Dfile.encoding=UTF-8 -cp out Main
```

使用 JDK 8 编译与运行；终端建议使用 UTF-8 编码（可通过 `echo $LANG` 查看）。
