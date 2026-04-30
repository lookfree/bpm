# BPM 模块集成方法

本仓库交付的是 jeecg-boot 的一个独立 Maven 子模块 `jeecg-module-bpm`，
不包含 jeecg-boot 主体源码。集成方需自行准备 **jeecg-boot v3.5.5**
源码，然后按以下步骤接入。

> 版本核实方法：根据 manage.iimt.org.cn 部署使用的 Spring Boot 2.7.10 +
> MyBatis-Plus 3.5.3.1 + Shiro 1.12.0 + JWT 3.11.0 + Knife4j 3.0.3 +
> FastJSON 1.2.83 + Aliyun OSS 3.11.2 等版本组合，比对 jeecg-boot 各 tag 的
> 顶层 `pom.xml`，**v3.5.5** 唯一全匹配。

## 1. 准备 jeecg-boot
git clone --depth 1 -b v3.5.5 https://github.com/jeecgboot/jeecg-boot.git
cd jeecg-boot

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
