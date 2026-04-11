# 第二阶段升级计划 — 建议升级但需谨慎

> 本文档记录需要谨慎处理的依赖升级项，涉及大版本跨越或 API 破坏性变更。
> 建议在第一阶段升级（已提交至 develop 分支）验证通过后，再逐项推进。

---

## 1. Kotlin 1.9.22 → 2.0.21（或 2.1.x）

| 项目 | 说明 |
|---|---|
| **当前版本** | 1.9.22 |
| **目标版本** | 2.0.21（先不跳到 2.3，从 2.0 稳定版起步） |
| **升级原因** | K2 编译器前端带来编译速度大幅提升；为后续 core-ktx 1.16+、Koin 4.x 等升级铺路 |
| **风险点** | K2 编译器行为有差异，部分边缘语法可能需要调整；kapt 和注解处理器兼容性需验证 |

### 注意事项
- Kotlin 2.0 引入全新的 K2 编译器前端，编译速度提升显著
- 项目使用了 Glide 的 `annotationProcessor`，需确认在 Kotlin 2.0 下正常工作
- 建议优先将 Glide 的 `annotationProcessor` 改为 KSP 方式（`ksp("com.github.bumptech.glide:compiler:4.16.0")`），以获得更好的 Kotlin 2.0 兼容性
- 升级后需全量回归测试，关注编译警告和错误

### 涉及文件
- `build.gradle.kts` — Kotlin 插件版本
- 可能需要调整 `app/build.gradle.kts` 中的注解处理器配置

---

## 2. Koin 3.5.3 → 4.x

| 项目 | 说明 |
|---|---|
| **当前版本** | 3.5.3 |
| **目标版本** | 4.0.0+ |
| **升级原因** | Koin 4.x 对 Kotlin 2.0 有完整支持；API 更现代化，性能优化 |
| **风险点** | 大版本升级，API 有破坏性变更；模块声明方式可能需要调整 |

### 注意事项
- Koin 4.x 需要 Kotlin 2.0+，**必须在 Kotlin 升级完成后才能进行**
- 主要 API 变更：模块定义语法、ViewModel 注入方式可能有调整
- 建议先查看 Koin 4.x 迁移指南：https://insert-koin.io/docs/4.1/support/releases/
- 全局搜索项目中所有 `org.koin` 相关引用，逐一验证兼容性

### 涉及文件
- `app/build.gradle.kts` — Koin 依赖版本
- 所有使用 Koin 注入的 Activity / Fragment / ViewModel / Application 类

---

## 3. Retrofit 2.9.0 → 3.0.0

| 项目 | 说明 |
|---|---|
| **当前版本** | 2.9.0 |
| **目标版本** | 3.0.0（2025-05-15 发布） |
| **升级原因** | 基于 OkHttp 4.12 重写，原生 Kotlin 支持，性能改进 |
| **风险点** | API 有破坏性变更；项目网络层是核心功能，需谨慎验证 |

### 注意事项
- Retrofit 3.0.0 内置 Kotlin 支持，引入了 Kotlin 传递依赖
- 底层升级为 OkHttp 4.12（从 3.14），但项目当前已使用 OkHttp 4.12，底层一致
- 主要关注点：Converter 工厂、CallAdapter、接口定义方式是否有变化
- 项目中使用了自定义的 `BiliSecurityCoordinator` 和 `NetworkManager`，需重点验证

### 涉及文件
- `app/build.gradle.kts` — Retrofit 依赖版本
- 所有 Retrofit API 接口定义文件
- `NetworkManager.kt` / `BiliSecurityCoordinator.kt` 等网络层核心类

---

## 升级顺序建议

```
第一阶段（已完成 ✓）
└── AGP + Gradle + AndroidX + Media3 + Coroutines 低风险升级

第二阶段
├── Step 1: Kotlin 1.9.22 → 2.0.21        ← 基础，其他依赖的前置条件
├── Step 2: Koin 3.5.3 → 4.x              ← 依赖 Kotlin 2.0
└── Step 3: Retrofit 2.9.0 → 3.0.0        ← 独立，可并行
```

每完成一步后，需通过编译 + 功能测试验证后再进行下一步。
