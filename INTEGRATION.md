# BPM 模块集成方法

本仓库交付的是 jeecg-boot 的一个独立 Maven 子模块 `jeecg-module-bpm`，
不包含 jeecg-boot 主体源码。集成方需自行准备 jeecg-boot v0.1.8
（或对应版本）源码，然后按以下步骤接入。

## 1. 准备 jeecg-boot
git clone https://github.com/jeecgboot/jeecg-boot.git
cd jeecg-boot
git checkout v3.6.3   # 与 jeecgboot-vue3 v0.1.8 对应的服务端 tag，需现场确认（C1）

## 2. 把本模块放入
将本仓库 `jeecg-module-bpm/` 整目录拷贝到 jeecg-boot 根目录：
cp -r /path/to/bpm/jeecg-module-bpm /path/to/jeecg-boot/

## 3. 修改 jeecg-boot 根 pom.xml
在 <modules> 下新增：
  <module>jeecg-module-bpm</module>

## 4. 修改启动模块依赖
在 jeecg-boot-module-system/pom.xml（或 jeecg-system-start/pom.xml，依发行版而定）
的 <dependencies> 中新增：
  <dependency>
    <groupId>org.jeecgframework.boot</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>${project.version}</version>
  </dependency>

## 5. 配置数据源 & Flowable
见 docs/superpowers/plans/2026-04-30-bpm-p0-scaffold-and-engine.md Task 7。

## 6. 配置 Shiro 放行路径
见 docs/superpowers/plans/2026-04-30-bpm-p0-scaffold-and-engine.md Task 8。
