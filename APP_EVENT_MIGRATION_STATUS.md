# MyBLBL 事件通信迁移状态

## 当前结论

`EventBus` 已从项目代码与构建依赖中移除。

当前事件通信分成两层：

- `MainNavigationViewModel`
  - 负责 `MainActivity` 范围内的主 Tab / 子 Tab / 返回键 / 菜单键 / 首页内容就绪事件
- `AppEventHub`
  - 负责应用级跨页面事件
  - 当前包含：
    - `UserSessionChanged`
    - `PlaybackProgressUpdated`
    - `EpisodePlaybackProgressUpdated`

## 已完成替换

- 主导航与子导航事件
  - 首页、分区、动态、直播、我的、首页时间线等页面已迁移到 `MainNavigationViewModel`
- 登录态 / 用户信息刷新事件
  - `signIn`
  - `updateUserInfo`
- 播放进度事件
  - `playUgc|...`
  - `playEpisode|...`

## 当前收益

- 不再依赖字符串事件名
- 事件生产者和消费者都能直接跳转定位
- 事件模型可测试、可扩展
- 构建依赖减少，页面生命周期更明确

## 本轮验证

- `.\gradlew.bat :app:compileDebugKotlin`
- `.\gradlew.bat :app:testDebugUnitTest`

## 后续建议

- 为 `AppEventHub` 补更多业务级测试，而不只验证事件分发顺序
- 继续拆解 `NetworkManager`，减少静态全局入口
- 把新的事件中心逐步迁入更清晰的 `core/event` 或 `core/app` 目录
- 在 README 或开发文档中补一条约束：禁止重新引入 `EventBus`
