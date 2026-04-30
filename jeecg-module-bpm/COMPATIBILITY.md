# 兼容性核实结果

| 项 | 值 | 说明 |
|---|---|---|
| 目标 jeecg-boot 版本 | v3.5.5 | 通过 root pom.xml 版本组合唯一识别 |
| BPM 模块 parent | spring-boot-starter-parent 2.7.10 | 不继承 jeecg-boot-parent |
| BPM 模块 Java 编译目标 | 1.8 | 跟 jeecg 保持一致，运行时支持 JDK 8+ |
| BPM 模块本机构建 JDK | 11 (Homebrew arm64) | macOS arm64 上 openjdk@8 brew core 没 arm64 包；JDK 11 编译 1.8 字节码无副作用 |
| Flowable 版本 | 6.8.0 | 与 Spring Boot 2.7.x 已验证兼容 |
| 测试用 MySQL | 8.0.27（Testcontainers） | 与生产环境一致 |
| 已知未确认项 | jeecg `act_*` 表是否已存在 | Task 11 中查 jeecg-boot/db/ 与启动后真实状态 |
