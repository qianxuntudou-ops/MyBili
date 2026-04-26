# 视频列表适配器统一基类重构计划

## 一、背景

项目中存在 4 个独立的视频列表适配器，继承体系混乱，大量重复代码（焦点处理、长按菜单、去重逻辑等），修改共性功能需要改多处且行为可能不一致。

## 二、现状分析

### 2.1 现有适配器一览

| 适配器 | 基类 | 数据模型 | 布局 | TvFocusableAdapter |
|--------|------|----------|------|--------------------|
| `VideoAdapter` | `BaseAdapter`（自定义） | `VideoModel` | `cell_video` | 通过 BaseAdapter 间接实现 |
| `DynamicVideoAdapter` | `ListAdapter`（系统） | `VideoModel` | `cell_video` | 直接实现 |
| `LaneItemAdapter` | `ListAdapter`（系统） | `LaneItemModel` | `cell_movie` | 未实现 |
| `HistoryVideoAdapter` | `ListAdapter`（系统） | `HistoryVideoModel` | `cell_video` | 直接实现 |

### 2.2 重复代码分布

以下逻辑在 2-4 个适配器中各自实现了一遍：

| 重复功能 | VideoAdapter | DynamicVideoAdapter | HistoryVideoAdapter | LaneItemAdapter |
|----------|:---:|:---:|:---:|:---:|
| 长按菜单（startLongPress/cancelLongPress/showCardMenu） | ✓ | ✓ | ✓ | ✗ |
| 点击防抖（longPressTriggered 标志位） | ✓ | ✓ | ✓ | ✗ |
| 焦点回调（onItemFocused / onItemFocusedWithView） | ✓ | ✓ | ✓ | ✓（简化版） |
| VideoCardFocusHelper.bindSidebarExit 绑定 | ✓ | ✓ | ✓ | ✓ |
| DPad 按键监听（keyListener） | ✓ | ✓ | ✓ | ✗ |
| 数据去重（deduplicate） | ✓ | ✓ | ✓ | ✗ |
| 数据追加（addData，含去重） | ✓ | ✓ | ✓ | ✗ |
| 移除不喜欢项（removeDislikedItem） | ✓ | ✓ | ✓ | ✗ |
| 移除屏蔽UP主（removeBlockedItems） | ✓ | ✓ | ✓ | ✗ |
| TvFocusableAdapter 实现 | ✓ | ✓ | ✓ | ✗ |
| DiffCallback | ✓ | ✓ | ✓ | ✓ |

### 2.3 受影响界面

| 界面 | 适配器 | 方向 |
|------|--------|------|
| 首页推荐 `VideoFeedFragment` | `VideoAdapter` | 竖向 |
| 分类列表 `CategoryListFragment` | `VideoAdapter` | 4列网格 |
| UP主空间 `UserSpaceFragment` | `VideoAdapter` | 竖向 |
| 频道视频 `ChannelVideoFragment` | `VideoAdapter` | 竖向 |
| 视频详情 `VideoDetailFragment` | `VideoAdapter`（内嵌） | 横向 |
| 播放器相关推荐 `PlayerActivity` | `VideoAdapter` | 横向 |
| 播放器相关推荐 `VideoPlayerFragment` | `VideoAdapter` | 横向 |
| 播放器浮层 `VideoPlayerOverlayController` | `VideoAdapter` | 横向 |
| UP主详情弹窗 `OwnerDetailDialog` | `VideoAdapter` | 竖向 |
| 详情页内嵌 `VideoDetailContentAdapter` | `VideoAdapter`（内部创建） | 横向 |
| 动态页 `DynamicFragment` | `DynamicVideoAdapter` | 3列网格 |
| 首页分区 `HomeLaneAdapter` | `LaneItemAdapter` | 横向网格 |
| 我的-历史记录 `MeListFragment` | `HistoryVideoAdapter` | 竖向 |

## 三、重构方案

### 3.1 目标架构

```
TvFocusableAdapter（已有接口）
       ↑
BaseVideoAdapter<T, VH>（新建）
  ├── 继承 ListAdapter<T, VH>
  ├── 实现 TvFocusableAdapter
  ├── 封装：焦点管理、点击/长按、去重、数据操作
  └── 提供模板方法供子类定制
       ↑
  ┌──────────────┬──────────────────┐
  VideoAdapter   HistoryVideoAdapter  LaneItemAdapter
  (VideoModel)   (HistoryVideoModel)  (LaneItemModel)
```

**关键决策：** 统一继承 `ListAdapter` 而非现有的 `BaseAdapter`。
- 现有 `BaseAdapter` 手动管理 `ArrayList<MODEL>` 和异步 DiffUtil，`ListAdapter` 已内置这些能力
- `ListAdapter.submitList()` 天然支持去重和增量更新
- 统一为 `ListAdapter` 后，所有适配器的数据管理方式一致
- `BaseAdapter` 中的"加载更多"功能通过单独的 `LoadMoreAdapter` 包装或由 Fragment 层处理

### 3.2 BaseVideoAdapter 设计

```kotlin
abstract class BaseVideoAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffCallback), TvFocusableAdapter {

    // ========== 焦点管理 ==========
    protected var focusedPosition = RecyclerView.NO_POSITION

    // ========== 回调接口 ==========
    var onItemClick: ((T) -> Unit)? = null
    var onItemFocused: ((Int) -> Unit)? = null
    var onItemFocusedWithView: ((View, Int) -> Unit)? = null
    var onTopEdgeUp: (() -> Boolean)? = null
    var onBottomEdgeDown: (() -> Boolean)? = null
    var onLeftEdge: (() -> Boolean)? = null
    var onItemDpad: ((View, Int, KeyEvent) -> Boolean)? = null
    var onItemsChanged: (() -> Unit)? = null

    // ========== 数据操作（含去重） ==========
    fun setData(list: List<T>, onCommitted: (() -> Unit)? = null)
    fun addData(list: List<T>)
    fun getItemsSnapshot(): List<T>
    fun removeItems(predicate: (T) -> Boolean)

    // ========== TvFocusableAdapter ==========
    override fun focusableItemCount(): Int = itemCount
    override fun stableKeyAt(position: Int): String?
    override fun findPositionByStableKey(key: String): Int

    // ========== 子类实现 ==========
    abstract fun itemKey(item: T): String
}
```

### 3.3 BaseVideoViewHolder 设计

将 ViewHolder 中的通用行为抽到基类：

```kotlin
abstract class BaseVideoViewHolder(
    rootView: View
) : RecyclerView.ViewHolder(rootView) {

    protected val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    protected var longPressTriggered = false

    // 长按菜单（子类覆盖以定制弹窗内容）
    protected open fun showCardMenu() {}

    fun startLongPress()
    fun cancelLongPress()

    // 焦点状态
    open fun applyFocusState(hasFocus: Boolean) {
        itemView.isSelected = hasFocus
    }
}
```

### 3.4 各适配器改造方案

#### VideoAdapter

**改造内容：**
- 继承 `BaseVideoAdapter<VideoModel, VideoViewHolder>`
- 删除：去重逻辑、removeDislikedItem/removeDislikedUpItems（改用基类 `removeItems`）
- 删除：焦点回调包装代码（基类统一处理）
- 删除：TvFocusableAdapter 实现代码
- 保留：`DisplayStyle` 枚举、`currentPlayingAid`、竖屏检测、播放状态动画、bindDefault/bindHistory
- `onBindViewHolder` 逻辑不变

**对外 API 变化：** 构造函数参数改为属性 setter（回调通过 setter 设定），或保持构造函数风格不变（向后兼容）。建议 **保持构造函数风格不变**，降低外部改动量。

#### DynamicVideoAdapter

**改造内容：**
- 继承 `BaseVideoAdapter<VideoModel, ViewHolder>`
- 删除：deduplicate、removeBlockedItems、removeDislikedItem、TvFocusableAdapter 实现
- 删除：ViewHolder 中的长按逻辑、焦点逻辑
- 保留：bangumi 标题/封面特殊处理

**对外 API 变化：** 基本不变，`setData`/`addData` 签名保持一致。

#### HistoryVideoAdapter

**改造内容：**
- 继承 `BaseVideoAdapter<HistoryVideoModel, ViewHolder>`
- 删除：deduplicate、removeBlockedItems、removeDislikedItem、TvFocusableAdapter 实现
- 删除：ViewHolder 中的长按逻辑、焦点逻辑、焦点状态管理（focusedPosition / setFocusedState / clearFocusMemory）
- 保留：bind 中的历史记录特有显示逻辑
- `toVideoModel()` 转换保留，长按菜单适配

#### LaneItemAdapter

**改造内容：**
- 继承 `BaseVideoAdapter<LaneItemModel, LaneItemViewHolder>`
- 新增：实现 `TvFocusableAdapter`（通过基类获得）
- 删除：setData 手动实现
- 保留：徽章颜色逻辑、`requestFirstItemFocus`（可上提到基类）
- 注意：此适配器不需要长按菜单，ViewHolder 不继承 BaseVideoViewHolder 的长按行为

### 3.5 不参与重构的适配器

| 适配器 | 原因 |
|--------|------|
| `HomeLaneAdapter` | 分区容器，管理多个子 RecyclerView，不是视频卡片列表 |
| `VideoDetailContentAdapter` | 多类型混合列表（头部+分集+相关视频），职责不同 |
| `DynamicUpAdapter` | UP主列表，不是视频列表 |

## 四、实施步骤

### 第 1 步：创建 BaseVideoAdapter（核心）

**文件：** `core/ui/base/BaseVideoAdapter.kt`

1. 创建 `BaseVideoAdapter<T, VH>` 抽象类
2. 实现 `TvFocusableAdapter` 接口
3. 封装通用数据操作：
   - `setData(list, onCommitted)` — 去重 + submitList
   - `addData(list)` — 去重 + 追加
   - `removeItems(predicate)` — 移除满足条件的项
   - `getItemsSnapshot()` — 快照
4. 封装焦点管理：
   - `focusedPosition` 状态跟踪
   - `clearFocusMemory()` 方法
5. 定义 `itemKey(item: T): String` 抽象方法
6. 提供边缘焦点回调的统一注册

### 第 2 步：创建 BaseVideoViewHolder

**文件：** `core/ui/base/BaseVideoViewHolder.kt`

1. 封装长按逻辑：`startLongPress()` / `cancelLongPress()`
2. 封装点击防抖：`longPressTriggered` 标志位
3. 定义 `showCardMenu()` 开放方法
4. 定义 `applyFocusState(hasFocus)` 方法
5. 绑定 `VideoCardFocusHelper.bindSidebarExit()`

### 第 3 步：改造 VideoAdapter（影响最大，优先处理）

1. 改继承为 `BaseVideoAdapter<VideoModel, VideoViewHolder>`
2. 实现 `itemKey()` 方法（复用现有 `videoKey` 逻辑）
3. 精简构造函数，保留所有现有参数以保持向后兼容
4. `VideoViewHolder` 继承 `BaseVideoViewHolder`
5. 删除重复的焦点/长按/去重代码
6. **测试所有使用 VideoAdapter 的界面**（见受影响列表）

### 第 4 步：改造 DynamicVideoAdapter

1. 改继承为 `BaseVideoAdapter<VideoModel, ViewHolder>`
2. 实现 `itemKey()` 方法（复用现有 `dynamicVideoKey` 逻辑）
3. `ViewHolder` 继承 `BaseVideoViewHolder`
4. 删除重复代码
5. **测试动态页**

### 第 5 步：改造 HistoryVideoAdapter

1. 改继承为 `BaseVideoAdapter<HistoryVideoModel, ViewHolder>`
2. 实现 `itemKey()` 方法（复用现有 `itemKey` 逻辑）
3. `ViewHolder` 继承 `BaseVideoViewHolder`
4. 删除焦点状态管理代码（基类处理）
5. 适配长按菜单（`toVideoModel()` 转换）
6. **测试历史记录页**

### 第 6 步：改造 LaneItemAdapter

1. 改继承为 `BaseVideoAdapter<LaneItemModel, LaneItemViewHolder>`
2. 实现 `itemKey()` 方法
3. 获得 `TvFocusableAdapter` 实现（通过基类）
4. ViewHolder 不继承 `BaseVideoViewHolder`（无长按需求），或继承但不触发长按
5. **测试首页分区**

### 第 7 步：BaseAdapter 加载更多迁移

现有 `BaseAdapter` 的加载更多能力（`showLoadMore` + `LOAD_MORE_TYPE`）需要处理：

- **方案 A（推荐）：** 将加载更多逻辑提取到 `BaseVideoAdapter` 中（可选启用）
- **方案 B：** 在 Fragment 层使用 `ConcatAdapter` 包装加载更多 item
- **方案 C：** 保持 `BaseAdapter` 不变，`VideoAdapter` 双重继承（不推荐，Kotlin 无多继承）

选择方案 A 或 B 取决于是否所有列表都需要加载更多。目前只有 `VideoFeedFragment`、`CategoryListFragment`、`ChannelVideoFragment` 使用了加载更多。

### 第 8 步：清理

1. 确认所有界面测试通过后，检查是否有未使用的旧代码
2. 如果 `BaseAdapter` 不再有任何子类，标记为 `@Deprecated` 或删除
3. 更新相关注释

## 五、风险与注意事项

### 5.1 高风险

| 风险 | 说明 | 应对 |
|------|------|------|
| 焦点行为回归 | TV 端核心体验，焦点丢失/跳转异常直接影响可用性 | 每个步骤完成后逐页测试 D-pad 导航 |
| 继承体系切换 | VideoAdapter 从 BaseAdapter 切换到 ListAdapter，数据管理方式完全不同 | 先充分理解 BaseAdapter 的 submitItemsInBackground 异步行为，确保 ListAdapter 的 submitList 覆盖相同场景 |

### 5.2 中风险

| 风险 | 说明 | 应对 |
|------|------|------|
| 加载更多兼容 | BaseAdapter 内置加载更多，ListAdapter 没有 | 第 7 步单独处理，不可跳过 |
| DiffCallback 行为差异 | BaseAdapter 手动 DiffUtil vs ListAdapter 内置 AsyncListDiffer | 注意异步 diff 的生命周期和取消行为 |

### 5.3 低风险

| 风险 | 说明 | 应对 |
|------|------|------|
| 对外 API 变化 | 外部 Fragment/Activity 的调用方式 | 保持构造函数签名和公开方法不变 |
| 性能 | AsyncListDiffer 可能与 BaseAdapter 的手动 DiffUtil 有细微性能差异 | 通常 ListAdapter 的实现更优 |

## 六、测试检查清单

每个步骤完成后，需验证以下场景：

### 通用检查
- [ ] 列表正常加载和显示
- [ ] D-pad 上下左右导航，焦点正确移动
- [ ] 焦点到达列表边界时，焦点正确传递到外层
- [ ] 点击项目，正确跳转
- [ ] 长按项目，弹出菜单（LaneItemAdapter 除外）
- [ ] 长按后松手，不触发点击
- [ ] 屏蔽UP主后，列表正确移除该项
- [ ] 不喜欢该视频后，列表正确移除该项

### VideoAdapter 专项
- [ ] 播放中状态（图标+标题高亮）正确显示
- [ ] 竖屏检测和标签正确
- [ ] 历史记录模式（HISTORY）进度条和时间正确
- [ ] 加载更多功能正常
- [ ] 竖向和横向布局均正确

### DynamicVideoAdapter 专项
- [ ] 3列网格布局正确
- [ ] 左边界焦点正确传递到UP主列表

### HistoryVideoAdapter 专项
- [ ] 焦点选中态（背景高亮）正确
- [ ] 历史记录时间和进度正确

### LaneItemAdapter 专项
- [ ] 首页各分区卡片显示正确
- [ ] 徽章颜色正确
- [ ] 分区间焦点切换正常

## 七、预计工作量

| 步骤 | 预计时间 |
|------|----------|
| 第 1 步：BaseVideoAdapter | 1-1.5h |
| 第 2 步：BaseVideoViewHolder | 0.5h |
| 第 3 步：改造 VideoAdapter | 1-1.5h |
| 第 4 步：改造 DynamicVideoAdapter | 0.5h |
| 第 5 步：改造 HistoryVideoAdapter | 0.5-1h |
| 第 6 步：改造 LaneItemAdapter | 0.5h |
| 第 7 步：加载更多迁移 | 0.5-1h |
| 第 8 步：清理 | 0.5h |
| 测试 | 1-2h |
| **总计** | **6-9h** |

建议分 2-3 个工作会话完成，每个会话完成 2-3 个步骤并测试。

## 八、不参与重构的部分

以下适配器因为职责不同，不纳入本次统一：

- `HomeLaneAdapter` — 分区容器，管理子 RecyclerView
- `VideoDetailContentAdapter` — 多类型混合列表
- `PlayerEpisodePanelEpisodeAdapter` — 播放器分集面板
- `DynamicUpAdapter` — UP主列表
- `SearchResultPagerAdapter` / `SearchItemAdapter` — 搜索相关
- `SeriesTimelineAdapter` — 时间线特殊布局
