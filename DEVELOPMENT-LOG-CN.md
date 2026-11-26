## 2026-11-26：v0.1.29 详情季卡首尾裁切针对性修复

- 用户继续实机反馈：季数较多时，焦点横移到最后一季，卡片右侧仍会被裁切；再移回第一季，左侧又会被裁切。v0.1.28 的该列表视口扩展方案未达到预期。
- 详情页同系列 LazyRow 恢复标准 Modifier.fillMaxWidth() 与左右 14dp contentPadding，取消该行上的 focusOverflowViewport 扩展，保持常规滚动视口与原始起始/末端位置。
- 季卡焦点背景倍率继续为 1.0625×，但按列表端点设置 TransformOrigin：第一季以左边缘中点为缩放锚点，最后一季以右边缘中点为锚点；缩放只向可用的列表内部增长。中间季仍以中心为锚点。
- 保留季卡宽 220dp、高 72dp、焦点 zIndex、字号、边框、圆角、阴影、焦点顺序和点击行为；不改 Logo、数据源、播放逻辑及其他组件。
- versionCode 30 / versionName 0.1.29。CI 与电视实机首尾来回切换分别验收。

## 2026-11-26：v0.1.28 滚动焦点视口边缘裁切修复

- 用户在 v0.1.27 电视实测后确认：详情页刚进入时第一季显示正常，但焦点移动到最后一季、再返回第一季时，焦点背景仍会被季卡 LazyRow 的实际裁切视口截断；排行榜等页面 FilterRow 的“全部”“推荐榜”在最左端获得焦点时也会被截左。由此确认 v0.1.27 的 contentPadding 方案没有覆盖焦点自动滚动后的所有边界状态。
- 新增共享 focusOverflowViewport 修饰符：内部滚动视口按指定像素向外扩展，但对父容器仍报告原测量尺寸；滚动内容加入等量留白，避免改变普通状态的控件起点和尺寸。
- FilterRow 的视口左右扩展 10dp、上下扩展 3dp；播放器控制横向滚动区左右扩展 12dp、上下扩展 4dp。留白放在 horizontalScroll 的滚动内容内部，首尾焦点项即使随焦点滚动到边缘也能保留完整背景。
- 详情季卡 LazyRow 左右视口扩展 12dp，start/end padding 从 14dp 调整到 26dp，抵消视口扩展，保持原有第一张/最后一张季卡的基准位置和可用宽度。
- 主页海报与热搜网格视口左右扩展 16dp，contentPadding 设置为 start 16dp / end 24dp；补偿扩展后仍保留之前 end 8dp 的有效宽度，因此 GridCells.Adaptive(minSize = 160.dp) 的列数和轨道宽度不变。详情选集网格切换为共享修饰符，保留已采用的左右 12dp 外扩与等量 contentPadding。
- 未调整焦点缩放倍率、普通状态尺寸、卡片点击/焦点区域、遥控顺序、页面数据、Logo、播放逻辑、全屏校准与 Activity 屏幕边界裁切。
- versionCode 29 / versionName 0.1.28。CI 编译/测试通过与目标电视在第一项、最后一项及来回切换的实际视觉验收是两个独立检查。

## 2026-11-26：v0.1.27 焦点比例校准与详情列表裁剪修复

- 根据已确认的字号增幅原则，逐项校准焦点背景的 X/Y 缩放倍率：主页导航 1.0588；筛选 Chip、通用操作按钮、播放器按钮 1.0667；详情季卡 1.0625；选集数字 1.0556；设置大按钮 1.0476；主页海报底板与图片 1.125。
- 保留焦点文字独立绘制；不再让七类旧控件统一使用 1.08 倍，也不让主页海报继续使用近似的 1.12 倍。
- 详情页同系列 LazyRow 增加 start/end 14dp、top/bottom 10dp 内容安全空间；选集 LazyVerticalGrid 增加 start/end 12dp、top 10dp，底部维持 24dp。
- 同系列季卡与选集卡片的外层 Box 根据焦点状态设置 zIndex=1，非焦点为 0，确保焦点项绘制在邻居上方；不关闭全屏 Surface 的屏幕边界裁剪。
- 选集 LazyVerticalGrid 的绘制视口横向各扩展 12dp，再用同额 start/end contentPadding 抵消；网格轨道仍按原始可用宽度计算，避免新增内边距改变 Adaptive 列数或单元格基准宽度。
- 保持各控件基础测量尺寸、遥控焦点顺序/点击范围、Adaptive(minSize=88dp) 列数规则、数据/播放回调、Logo 和播放器状态机不变。Android CI 和目标电视上的左/右/上/下边缘实测仍是验收环节。
- versionCode 28 / versionName 0.1.27。

## 2026-11-26：v0.1.26 主页海报焦点放大试验

- 按用户确认的规划，本轮仅处理主页剧集海报的焦点状态，不改变非焦点状态的布局、字号、图片尺寸或网格列数。
- `CatalogCardView` 增加焦点绘制层级，允许放大后的焦点海报覆盖相邻卡片；底板与海报图使用独立视觉缩放约 1.12 倍。
- 标题 16→18sp、副标题 13→14sp，并独立绘制；焦点文字偏移仅影响绘制位置，不改变网格项目的测量尺寸。底板与图片均以顶部为缩放锚点，减少文字/图片之间的视觉错位。
- 不改海报资源、2:3 图片比例、点击回调、封面缓存、播放逻辑、其他焦点控件或全屏校准。目标电视仍需检查图片边缘、上下行覆盖与文字清晰度。
- versionCode 27 / versionName 0.1.26；CI 通过后仍需实机验收。

## 2026-11-26：v0.1.25 推广主页导航焦点视觉规范

- 用户确认 v0.1.24 的主页导航效果清晰且比例合适，要求将方案推广到其他可点击焦点控件。
- 筛选 Chip、通用操作按钮、设置大按钮、详情页季数/选集按钮与播放器控制按钮统一改为：焦点底板独立 1.08 倍放大；文字与底板分层绘制，不继承底板缩放；焦点字体使用 Medium（500）并小幅增大。
- 字号根据控件基线分别调整，避免把小控件统一放到 18sp：筛选/常用操作/播放器 15→16sp；季数主标签 16→17sp、集数 11→12sp、季标题 12→13sp；选集数字 18→19sp；设置标题 21→22sp、说明 15→16sp、箭头 34→36sp。主页导航维持 18sp / Medium。
- 保留各控件原有布局测量尺寸、焦点/点击区域、焦点配色、描边与阴影；海报内容卡、系统开关、搜索输入、Logo、播放状态机、自动隐藏与画面校准不改。
- versionCode 26 / versionName 0.1.25；等待 CI 验证后在电视上做全页面焦点回归。

## 2026-11-26：v0.1.24 回调主页导航焦点字号

- v0.1.23 已通过 Android CI 并正式发布；电视实机反馈确认焦点文字比之前清晰，但 20sp 显得过大。
- 仅将 `NavigationItem` 的焦点字号从 20sp 回调到 18sp；Medium（500）字重不变，文字仍独立绘制，不继承底板的图层变换。
- 保留焦点底板 1.08 倍放大（背景、描边、圆角和阴影），失焦文字仍为 17sp；金色焦点样式、布局测量尺寸、遥控焦点范围不变。
- 筛选、常用操作、设置、播放器、Logo、全屏校准和播放逻辑均未改。
- versionCode 25 / versionName 0.1.24；CI 通过后需在同一电视上复验字号比例。

## 2026-11-26：v0.1.23 主页导航焦点尺寸试验

- 用户确认采用“焦点按钮区域放大约 8%，文字不加粗而是直接增大字号”的方向；本轮先限定在主页左侧导航，不将试验扩展到其他控件。
- `NavigationItem` 将焦点底板（背景、描边、圆角和阴影）放在独立视觉层，获得焦点时 scaleX/scaleY=1.08；布局测量尺寸和遥控焦点范围仍为原来的 190dp × 46dp，避免焦点项改变后续导航项间距。
- 焦点文字使用独立绘制层，字号由 17sp 调至 20sp，字重使用 Medium（500），不随底板缩放；失焦文字保持 17sp，选中但失焦的字重逻辑保留。
- 其他筛选按钮、常用操作按钮、设置大按钮、播放器控制按钮、Logo、配色、播放逻辑和全屏画面校准均不改。
- versionCode 24 / versionName 0.1.23；Android CI 构建通过后仍需电视实机判断整体比例与文字清晰度。

## 2026-11-26：v0.1.22 移除按钮焦点整体缩放

- 根据电视实机观察到的焦点态文字锐度下降，移除首页导航、筛选按钮、普通操作按钮、设置大按钮和播放器控制按钮在获得焦点时的 1.025/1.035 倍 graphicsLayer 整体缩放。
- 文字不再随按钮进行非整数倍图层变换；不调整字号、字重、金色背景、反色文字、描边、阴影、控件尺寸和导航顺序。
- 只删除按钮焦点缩放，保留 MainActivity 与 TvPlayerScreen 中负责界面/视频全屏画面校准的 graphicsLayer 变换。
- versionCode 升至 23 / versionName 升至 0.1.22；Android CI 和目标电视远距离观看效果仍需分别验收。

## 2026-11-26：v0.1.21 放大主页透明底 LOGO

- 用户反馈主页左上角 Logo 视觉尺寸偏小；复核 v0.1.20 源码确认图片显示尺寸为 58dp。
- 仅将 MainActivity.kt 中主页专用透明底 tv_brand_mark 显示尺寸调整为 68dp，约放大 17%；保留原始位图、透明背景和 ContentScale.Fit，不重新绘制、不替换资源。
- 桌面应用图标、TV banner、右侧三行字标、侧栏宽度、导航焦点、播放器及其他页面均不改。
- versionCode 升至 22 / versionName 升至 0.1.21；CI 通过与目标电视视觉效果仍需分别确认。

## 2026-11-26：v0.1.20 恢复高光立体原始位图 LOGO

- 依据用户重新提供的透明底原图制作栅格素材，直接缩放源图，不用矢量路径临摹；保留果形环带、叶片、中心播放三角、红橙渐变及强反射高光。
- `tv_brand_mark.webp` 为主页专用透明底素材；`tv_icon_launcher.webp` 将同一源图叠放在黑色圆角底板上，四周只保留轻微暖色边缘光。两处不再共用带黑底的图标文件，也不再用扁平矢量代替原始材质。
- TV banner、页面排版、控件焦点、播放器状态机及自动隐藏计时不变。
- versionCode 升至 21 / versionName 升至 0.1.20；同步 CI 文件名、正式 Release 工作流、README、CHANGELOG 和交接文档。CI / 目标电视实机显示仍待验收。

## 2026-11-26：v0.1.19 圆角控件反馈与主页 LOGO 分离

- 在导航、筛选、内容卡片、详情季卡/选集、设置动作和播放器控制项的 clickable 区域外包裹对应圆角裁剪，使点击/焦点交互反馈服从控件既有轮廓，保留既有高亮颜色、描边、放大和阴影。
- 桌面图标沿用 v0.1.18 已确认的发光果形和播放三角，只将完整图形等比例缩小约 10%，增加黑底留白。
- 主页 LOGO 不再复用桌面图标：从原素材导出透明底独立果形，去掉黑色方形底板，显示尺寸从 52dp 增至 58dp；不修改右侧三行文字或 TV banner。
- 播放状态机、遥控导航顺序和 500ms/3s/5s 分场景隐藏策略不变。
- versionCode 升至 20 / versionName 0.1.19；同步 CI artifact、正式签名发布工作流和当前版本文档。CI/目标电视实机验收仍待结果。

## 2026-11-26：v0.1.18 统一应用图标与首页品牌 LOGO

- 用户确认采用带播放三角的发光果形设计；取消版只保留为备选设计稿，不作为应用品牌。
- 应用桌面图标与首页左上角 LOGO 继续复用 `tv_icon_launcher.webp` 这一资源，因此只替换单一图片即可统一两处视觉，避免 Logo 与桌面图标长期分叉。
- 新素材使用纯黑背景，果形主体适度放大并保留叶片、渐变光泽与中心播放三角；不添加外层方框。TV banner 本版暂不修改，控制变更范围。
- 版本升级为 versionCode 19 / versionName 0.1.18；同步 CI artifact、正式签名发布工作流和所有当前版本文档。图标边缘/大小在实际电视桌面上的表现仍需实机确认。

## 2026-11-25：v0.1.17 修正按键 API 编译问题并重跑正式候选

- v0.1.16 首轮 Android CI 的应用编译报告：当前 Compose 版本没有 nativeKeyEvent 符号，因此单元测试步骤失败，Lint/APK/签名都被正确跳过；v0.1.16 不创建正式 Release。
- 删除对 nativeKeyEvent 的引用。焦点进入选集使用页面内一次性 episodeEntryPending 状态锁防止长按遥控右键重复触发；滚动、等待目标集 FocusRequester 挂载、请求焦点完成后释放锁，不触碰播放器菜单定时器。
- 保持既定交互范围：隐藏播放器菜单按确认键后首焦点落在播放/暂停、详情页默认焦点是继续播放并按历史集定位、季卡总集数固定放在第一行；图标素材保持不动。
- versionCode 升至 18 / versionName 0.1.17；同步 Android CI、正式签名 Release 工作流和累计文档。

## 2026-11-25：v0.1.16 长辈友好焦点与选集定位

- 严格控制修改范围：本版本不修改图标、首页 Logo、TV 横幅、播放状态机或分场景自动隐藏策略。
- 播放器菜单隐藏时按确认键，仅把菜单召出并将焦点置于第一个“播放/暂停”按钮；使用一次性标志抑制自动焦点触发的首个 5 秒续时回调，让本次召出继续按原来 3 秒隐藏。已有的切集 500ms、普通快退/快进 3 秒、控制栏操作 5 秒规则不变。
- 详情页在选集数据可用后显式聚焦“继续播放”。从收藏/返回等左侧操作控件向右进入选集区时，优先选择当前系列观看历史中的 episode；若找不到当前季对应集或没有历史，则使用第 1 集。目标集可能在可视区外时先滚动到目标 index，等待对应 FocusRequester 进入 composition 再请求焦点。
- 同系列季卡从 178×70dp 调为 220×72dp，第一行以 SpaceBetween 并排显示季标签和总集数，第二行显示单行省略的季标题，避免三行排版挤掉最下面的集数。
- versionCode 升至 17 / versionName 0.1.16；同步 CI Artifact、正式签名 Release 工作流、README、CHANGELOG 与 HANDOFF-CN.md；图标素材保持 v0.1.15 不变。

## 2026-11-25：v0.1.15 恢复第二款原始图标设计

- 找回此前生成的九款图标设计底稿；用户指定第二款 Glossy Fruit Play Icon，不再使用当前误恢复的旧款图形，也不继续用 VectorDrawable 手工临摹。
- 将第二款原始设计制作为 Android 可加载的 WebP 图片素材，桌面图标与首页 LOGO 直接引用该素材；TV banner 复用同一个原始图形并排版，保持深色圆角底、果形渐变、叶片、播放三角与柔和光影。
- AndroidManifest 的 icon/banner 与 MainActivity 首页标志改为直接引用 WebP；删除旧的手绘 VectorDrawable 图标和 banner，避免再次误用。
- 保留 v0.1.14 对详情页标题、“同系列”、“选集”暖白字和“选中但失焦不显示常驻金边”的修复。
- versionCode 升至 16 / versionName 升至 0.1.15；同步 Android CI artifact、正式签名 APK、SHA-256 和发布工作流。
- CI 成功后核验正式 Release 资产；尚未在目标电视上实测的桌面图标尺寸与显示细节必须明确标记为待实机验收。

## 2026-11-25：v0.1.14 恢复暗色品牌视觉并修正焦点/文字状态

- 复盘 v0.1.13 的图标：原生矢量虽能编译，但误将品牌改成冰青浅色底和横向拉宽的果形播放带，与用户原先确认的黑底、边角微光和原始红果图形不符。
- 将系统图标、首页 LOGO 和 TV banner 的视觉统一回深石墨黑底；使用低透明度边角光效、红果形主体与绿色叶片；图形组只做等比例缩放，杜绝 X/Y 非等比例变形。
- 复盘详情页黑字：主标题、“同系列”和“选集”没有声明颜色，而根容器只是 Box，未把 Material3 暗色主题的 onSurface 色传给子树。根容器改为显式提供暖白 contentColor 的 Surface，并为详情三个标题增加显式暖白色，统一修复默认文字颜色遗漏。
- 清理选择态描边：导航、筛选、季卡、选集和常用按钮不再给失焦的已选中项目保留常驻金边；只有实际焦点才使用琥珀金填充/浅金描边，失焦选中态保留深色抬升面板。
- versionCode 升至 15 / versionName 升至 0.1.14；同步 Android CI Artifact 和正式发布工作流文件名，保留单一累计 DEVELOPMENT-LOG-CN.md 作为开发日志。
- 构建后将核对 Android CI、签名与 SHA-256；实际电视遥控焦点、字色与图标显示仍需在目标设备验收。

## 2026-11-25：v0.1.13 修复图标资源解析与启动闪退

- 复盘 v0.1.12：Manifest 的 `@drawable/tv_icon` 与首页 Compose 同时引用新增的 `drawable-nodpi/*.webp` 位图。桌面图标出现系统默认 Android 占位图；首页又在 Activity 初次组合时同步加载该图，可能因资源解析异常直接退出。
- 移除 v0.1.12 新引入的两张 WebP，系统图标、首页品牌标志和 TV banner 全部恢复为原生 Android VectorDrawable，避免依赖额外位图解码；首页与 Manifest 复用同一 `tv_icon` 资源。
- 保留冰青底与红橙金果形播放符号的品牌方向，以原生矢量路径绘制，确保 Android 6.0+ 资源兼容。
- 版本升级至 versionCode 14 / versionName 0.1.13；同步 Android CI artifact 命名、签名校验与正式 Release 工作流。CI 将验证资源编译、测试、Universal APK、签名与 SHA-256；启动实机仍需确认。

## 2026-11-25：v0.1.12 渐变品牌与焦点逻辑回归

- 按用户选择的第三款图形语言保留果形丝带、播放三角和叶片；为提升电视桌面暗色背景上的可见度，使用高明度冰青渐变底搭配红橙金渐变标志，并同步制作 TV 横幅。
- 首页左上角使用同一品牌图形加“红果 TV”字标，避免应用图标与首页品牌符号不一致。
- 修复导航/筛选/详情选季/选集：只在真实焦点态填充琥珀金并反色文字，选中但失焦时回到深色面板和浅色文字；移除白色粗框，保存态仅保留细金色轮廓。
- 将常见 TV 操作按钮从 Material 默认金色填充改为专用组件，统一正常态、焦点态、禁用态。
- 依据 v0.1.10 实际源码恢复播放器分场景超时：切集 500ms、菜单召出/快退快进 3s、控制栏焦点/操作 5s、默认计时参数 650ms；将诊断操作并入播放控制同一水平焦点组，方向键有机会交给 Compose 焦点搜索。
- 播放按钮尺寸由至少 124×58dp 收到至少 88×46dp；焦点放大收敛为约 3.5%，不再依靠 4dp 白框。
- versionCode 升至 13 / versionName 升至 0.1.12；Android CI、正式发布工作流、README、交接索引和更新日志同步。CI 通过后仍须电视实机确认实际遥控效果。

## 2026-11-25：v0.1.11 影院石墨主题与长辈友好播放器遥控

- 用户反馈父母在播放时打开控制菜单后难以辨认当前选中按钮。复核 TvPlayerScreen 后确认普通 TextButton/Material Button 没有一套明确一致的焦点外观，且菜单未聚焦按钮时左右键仍用于快退/快进，要求先按上下键才能进入按钮导航。
- 播放菜单打开时直接请求“播放/暂停”按钮焦点；OK 负责打开并聚焦菜单，不再在打开菜单时隐式切换播放状态。按钮获得焦点后，左右键交给 Compose 焦点搜索；菜单聚焦期间上下键不再误跳出控制区。
- 播放器控制按钮改为统一的大尺寸自定义 Surface：焦点琥珀金填充、暖白 4 dp 描边、深色粗体文字和约 7.5% 放大；按钮宽度设定可读下限，长菜单继续横向滚动。
- 菜单无操作收起时间调整为 12 秒；焦点移动与按键操作会重置计时，避免用户辨认按钮时菜单过快消失。
- 全局主题从荧光绿/亮黄混用调整为深石墨（#0D131A）、墨蓝灰（#18212B）、抬升面板（#24313D）、古铜金（#C89452）、琥珀金焦点（#F0B65A）、暖白文字（#F4F1E9）和雾灰辅助文字（#A7B0BA）。导航、筛选、卡片、季数、选集、设置、接力与画面校准按钮按统一焦点规范调整；测试图的红色边界标识保留。
- versionCode 升至 12，versionName 升至 0.1.11；README、CHANGELOG、交接索引和 Android CI Artifact 文件名同步。自动化 CI 与目标电视实机焦点行为待验收。

## 2026-11-25：v0.1.10 按电视遥控器重做画面校准

- 根据用户实机反馈，v0.1.9 校准页把界面/视频缩放和平移按钮挤在两个深色卡片里，焦点颜色对比不够，方向操作移动整个矩形而非改变指定边缘；该交互不适合电视遥控器，应整体重做而不是微调样式。
- 对照 Kodi 官方 Display Calibration 说明，改为先等比例缩小到合适范围，再独立校准四条边。每次按“上边/下边/左边/右边扩展”只向外移动指定的一条边，边缘之间互不联动；每一步为屏幕对应尺寸的 0.5%，可一直扩展到外边界。等比例缩放范围 70%–100%、步进 5 个百分点。
- 设置主页变成单列分层：设备接力保持直接开关；界面校准和视频画面校准分别进入独立全屏测试页；提供本项重置和一键全部重置。设置页本身固定不缩放，避免误调后无法找回设置。
- 两种测试页使用高饱和双色场、粗黑/白双边框、红色角标、九宫格和中心圆/十字，以便看到屏幕边缘及识别拉伸；遥控器焦点改为亮红底、白色粗边框、放大高亮，不再依赖细微深浅变化。
- 用同一组 DisplayCalibration.bounds 将测试框位置映射至界面层与视频 TextureView；扩边参数分别持久化到本机，旧版 95% 等比例值仍复用原设置键，旧的平移值不再使用。
- 新增测试覆盖：单边调整不会移动其余三边、四边可分别扩展到全屏边界、范围裁剪与界面/视频参数彼此独立。视频画面非等比例扩边可能造成变形，这是有意提供的边缘校正；用户若不需要，保持四边为 0 即仍为等比例缩放。
- 当前 versionCode 11 / versionName 0.1.10。发布说明已同步为四边独立扩展与高对比测试图，不再沿用旧版整体平移文案。最终 Android CI、正式签名与校验通过，后续如重跑需以带最终标记提交的构建和发布工作流为准；电视实机仍需确认按钮焦点和实际图像边缘。

## 2026-11-25：v0.1.9 老电视画面校准

- v0.1.9 发布候选提交标记：界面/视频校准源码、测试、CI 与正式签名 APK 已在同一候选版本验证；发布以 workflow_run 校验并创建 GitHub Release 为准。

- 根据旧电视屏幕边缘裁切/尺寸异常的需求，参照 Kodi 的界面 Zoom 与 Video Calibration 分离设计，以及 Android TV 对 overscan-safe area 的建议，决定分别校准界面和视频，不用单一缩放值把控制面板和视频内容混在一起。
- 设置页增加“界面校准”：70%–110% 缩放、水平/垂直各 ±120 dp 偏移；方向按钮每步 8 dp，缩放按钮每步 5 个百分点。该校准作用于首页、详情、诊断、设置及播放控制/诊断叠层，不缩放视频本身。
- 设置页增加“视频画面”：70%–160% 等比例缩放、水平/垂直各 ±320 dp 偏移；视频表面和 UI 叠层分离，避免缩放视频时连带放大播放菜单/诊断文字。六个参数存入本机 SharedPreferences，实时应用、下次启动保留、各自可恢复默认。
- 视频校准不再直接变换内层 TextureView，而是在全屏 AndroidView/PlayerView 视口上使用 Compose graphicsLayer 映射同一组四边坐标；PlayerView 内部仍使用 fit 保持默认 100% 的原始等比例适配。TextureView 仍作为可随视口变换的画面表面。只有非等比例扩展边缘时，最终视口内容才可能被拉伸。仍需在旧电视检查掉帧、黑屏和字幕/缓冲指示的变换表现。
- 新增 DisplayCalibration 的范围裁剪/默认值单元测试；已更新 app versionCode 10 / versionName 0.1.9、Android CI Artifact 版本名与正式发布工作流。当前不能提前声称编译或电视播放已通过，待 CI 结果和目标电视实测确认。

## 2026-11-25：v0.1.6 系列剧跨季选择

- 用户实机反馈：打开系列剧的某一季后，详情只显示当前季选集，其他季没有归纳到同一系列中。
- 对照开心果的 `SeriesScreen.kt`，确认 SDK 与 AppModel 已提供 `catalog.seasons(id)`、季序号排序和所有季共享季列表；NEXT 电视界面的 `DetailScreen` 却完全没有渲染季选择入口。
- 在选集标题上方增加“同系列”季数栏，展示每季序号、标题与集数；当前季高亮，遥控器聚焦可见；点击已上线季直接切换详情 ID，避免返回首页重新搜索。
- 未上线或无分集季依旧显示为不可选，避免打开没有选集的详情页。将 `SeriesPage.seasonsLoading` 变为 Compose 可观察状态，让“正在获取同系列季信息”提示能随数据加载刷新。
- 保留 v0.1.5 的封面 URL 回退与诊断改动；版本提升到 v0.1.6 / versionCode 7。
- 完整 Android CI、Universal APK、签名/SHA-256 和 v0.1.6 Release 仍需以本次 Actions 最终结果确认为准；季列表与遥控器焦点需在目标电视上实测。

## 2026-11-25：v0.1.5 TV 封面加载修复

- 用户实机反馈 v0.1.4 所有封面都不显示，按最高优先级排查封面 URL → HTTP → 解码 → 缓存 → Compose 显示链路。
- 对比旧版 `hongguoTV/TvArtworkLoader.kt` 后，确认 NEXT 的 `CoverStore` 漏迁了旧 TV 已有的 CDN URL 回退：byteimg/fqnovelpic 的 `~tplv-obj.image` 需要优先尝试 `~tplv-noop.jpeg`，novel-pic/novel-static 需要稳定 JPEG 变体。NEXT 此前只修改 Accept 头并直接解码原 URL；当 CDN 返回设备不支持的格式时，所有封面会静默失败。
- 恢复旧 TV 的候选 URL 顺序，先尝试 JPEG，再保留原始签名地址；HTTP URL 优先升级 HTTPS，失败时再回退原始 URL。
- 增加 User-Agent、重定向跟随、候选逐一下载/解码、坏磁盘缓存清理、缓存压缩/替换结果检查，以及 16 MiB 下载限制。
- 增加封面诊断计数和最近错误摘要；诊断报告不会输出完整 URL/query 签名。
- 新增 CoverUrlTest，覆盖 byteimg 动态图片、novel-pic、HTTP→HTTPS 与空/不支持协议场景。
- 版本提升为 v0.1.5 / versionCode 6。源码变更不视为修复完成，仍以 Actions 和用户电视实机结果验证。

- v0.1.10 最终发布候选标记：界面与视频校准分别使用全屏高对比测试图；先等比例缩放，再独立向外扩展指定边缘；界面/视频设置独立持久化和重置。最终交付以本提交对应的完整 Android CI、正式签名及 SHA-256 校验成功并实际生成 GitHub Release 为准，电视实机验收仍由设备端确认。

# 开发日志（持续累积）

## 2026-11-24：v0.1.4 将最低系统版本下调至 Android 6.0

- 按家庭电视兼容目标，先尝试一个 Universal APK 覆盖全部设备，不维护双仓库或两套业务代码；最低安装版本由 Android 9.0/API 28 下调至 Android 6.0/API 23。
- 审计源码发现 PhoneDiagnosticClient 与 Player 使用 java.time.Instant；加入 Core Library Desugaring 2.1.5，避免 API 23 设备在诊断时间戳和睡眠定时器路径触发缺失类问题。
- 保持 Media3 1.11.1、自维护 openhongguo SDK AAR、CENC/播放器与当前 ABI 配置，不为兼容目标盲目降级播放依赖。
- Android CI 新增读取最终 Universal APK Manifest 的门禁，要求最低 SDK 精确为 API 23；若传递依赖要求更高 API、编译/Manifest 合并不接受 API 23，CI 应失败而不是宣称兼容。
- 当前代码目标是 Android 6.0 系统安装兼容；Widevine/DRM、视频编码、SoC 解码器和遥控器焦点表现仍需在小米 E55A 与东芝 75U6900C 实机逐项验证。CI 尚未运行时不宣称 API 23 构建已通过。


## 2026-11-24：v0.1.3 修复播放空响应与统一 SDK 构建

- 对照 NEXT 初版和开心果历史提交后确认：两边 `Catalog.resolvePlayback` 入口基本相同，差异是 NEXT 只依赖官方 Maven SDK；开心果早在 2026-11-21 就加入 V5 空响应后的签名 `video_tab` GET 恢复，并通过工作流注入本地 AAR。
- Android CI 固定检出 `xiaobei206/openhongguo@1e9334185a46033bcc4e3e30fc41f649958b1389`，执行诊断补丁，运行 `go test ./...` 和 mobile core 测试，再使用 `bindings/mobile/build.sh android` 构建 AAR。
- NEXT 的测试、Lint、Debug APK 和正式签名 APK都显式使用这份 AAR；缺少 AAR 参数时 APK 打包会失败，避免静默退回官方 Maven SDK。
- 该 SDK 版本包含 V5 空响应后的签名、无请求体 `video_tab` 回退、同一身份和当前分集校验，以及设备激活、EGDI 与最新签名实现。
- 诊断补丁仅输出 HTTP 状态、去 query 接口路径、响应长度、Content-Type/Encoding 与脱敏 profile fingerprint；不会记录原始标识、Cookie、Token、签名或完整响应体。
- 版本提高到 v0.1.3 / versionCode 4；同步 CI Artifact 与发布工作流文件名。
- 源码提交不等于验证通过；需核对本次 Actions 及在目标电视上重试此前返回空 200 的同一分集。

## 2026-11-24：v0.1.2 电视遥控器播放器、热搜和日期标签

- 新增 `TvPlayerScreen.kt`，以电视遥控器为中心管理 HUD 可见状态与焦点状态；菜单隐藏时左右快退/快进、上下切集；OK 第一次显示菜单，下一次暂停并把焦点交给播放按钮；控制栏获得焦点后允许方向键导航。
- 播放初始和切集时菜单短暂显示约 0.5 秒；手动 OK 唤出菜单 3 秒隐藏，控制按钮交互后 5 秒隐藏。菜单隐藏时控制按钮直接不进入 Compose 树，避免残留按钮。菜单显示时返回先隐藏；隐藏时返回提示二次确认退出。
- 诊断入口中文命名为“播放诊断”，悬浮框保留英文 `STATS FOR NERDS`，读取已有 `PlaybackDiagnostics` 的真实吞吐、编码、解码器、分辨率、帧率、码率和丢帧信息。
- 搜索空查询时复用 `AppModel.hot` 和开心果 SDK 热搜结构；展示排名、榜单名、热度/搜索分数，并在热搜加载失败时提供重试。
- 新剧日期按钮调用迁移自开心果的 `dayLabel(date, todayKey())`，日期筛选逻辑保持原样。
- 版本提升到 0.1.2 / versionCode 3；CI Artifact 与正式 Release 工作流更新为 v0.1.2。Actions Run [#45389071879](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45389071879) 已通过单测、Lint、Debug 构建、ABI 检查、正式签名与 SHA-256 校验；[v0.1.2 正式 Release](https://github.com/xiaobei206/hongguoTV-next/releases/tag/v0.1.2) 已发布。播放和遥控器仍需目标电视真机验收。

## 2026-11-24：v0.1.1 播放空响应修复与接力开关

- 根据电视发回的诊断，列表/详情请求正常，但播放地址接口 `/novel/player/video_model/v1/` 返回 HTTP 200、空响应体，导致 `ANONYMOUS_LIMITED`。上游 openhongguo 在 2026-11-24 发布 0.4.1，说明 0.4.0 在服务端于 11 月 23 日起拒绝带模拟器状态值的签名，换匿名设备也无法恢复。
- 将 `openhongguo` 从 0.4.0 升级至 0.4.1；版本升级并不会自动证明实机播放已恢复，需等 CI 构建并由用户实测。
- 播放器失败界面增加重新尝试按钮；若诊断码支持匿名设备刷新，则显示“更换匿名设备重试”。
- 设备接力配置存入 `tv_handoff_identity` 的 `enabled` 字段，首次安装默认 `true`；设置页和设备接力页共用一个开关，关闭时取消 NSD 广播且不接收 TCP 播放任务。应用进入后台时仍停止广播，以减少后台活动。
- app 版本提高为 0.1.1，CI Artifact 名与 SHA 文件同步更新。正式 Release 只在本次 Android CI 成功后由 `.github/workflows/publish-v0.1.1.yml` 发布。

## 2026-11-24：仓库审计、工程基础与核心迁移

### 仓库审计
- `hongguoTV-next` 初始为空仓库，默认分支为 `main`；新版所有写入仅发生在该仓库。
- `kaixinguo-android` 与旧 `hongguoTV` 均包含要求核查的主要模型、播放、CENC、缓存和诊断/接力代码。旧版主 UI 真实入口仍是大型 `MainActivity.kt` 和 RecyclerView adapter 流程，不能因 UI 目录存在就认为它运行中。
- 开心果项目 Gradle version catalog 使用 AGP 9.5.1、Kotlin 2.4.21、Compose BOM 2026.09.00、Activity 1.13.0、Core 1.19.1、lifecycle 2.11.0、Media3 1.11.1、openhongguo 0.4.0、coroutines 1.11.0、JUnit 4.13.2、org.json 20250517；Gradle Wrapper 9.8.2。新版沿用参考项目实际使用的 Compose BOM 2026.09.00、Core 1.19.1、Lifecycle 2.11.0 等版本；CI 实际验证出它们的 AAR 元数据要求 `compileSdk 37`。为避免未经验证地降级多个核心库，改为从 SDK Beta 通道安装 API37 编译平台，`targetSdk` 仍为 36，应用运行时目标行为不随 compileSdk 提高而改变。Android 17/API37 当前为 Beta，因此此构建基线的编译平台仍是预览通道，需在后续稳定版发布后再复核。
- 开心果的 `Feed` 具备 ID 去重、独立游标、加载/错误状态、退避重试及首页缓存恢复；`AppModel` 分别缓存首页、榜单、新剧和搜索 Feed。新版本沿用这些模型与状态语义，而非另造一套数据访问层。
- `CoverStore` 包含 96 MiB 内存 LRU、磁盘缩放 JPEG、同 URL 请求共享与并发 IO 限制。播放核心由 Media3 ExoPlayer、`StreamRegistry`、`CencDataSource` 和 CENC 解密组成。
- 旧 TV 服务在 NSD 上广播 `_hongguotv._tcp.`，TCP 一行一个 JSON，协议版本 1。播放请求字段：`protocol`、`type=play`、`transferId`、`targetDeviceId`、`seriesId`、`episode`、`positionMs`；回复字段：`protocol`、`transferId`、`accepted`、`message`、`deviceId`。服务在 Activity 前台期间发布可用状态，接力回复等待实际 Player 状态。此协议现在已提取纯编解码器并补充单元测试，仍需要真机互通验证。
- 两个参考公开仓库目前都有被跟踪的根目录 JKS/签名配置。新版不复制或继承其中任何密钥，使用独立签名配置；私密签名材料在新版由 `.gitignore` 排除。

### 本阶段提交内容
- 独立工程、Gradle Wrapper/版本目录、TV Manifest、根目录签名配置读取、`.gitignore`、CI 及中文文档。
- 迁移开心果的 `AppModel`、`Card`、`Catalog`、`Danmaku`、`Feed`、`PlaybackFailure`、`Player`、`Seasons`、`SeriesPage`、`Session`、`Store`、`CoverStore`、`Cenc`、`ExoVideo`、`PlaybackDiagnostics`、`PlaybackService` 和 `Streams`，包名全部改为新版命名空间。
- 迁移旧 TV 的 `TvHandoffServer` 与 `PhoneDiagnosticClient`；没有改动旧 TV 仓库。没有将开心果手机端的 `PhoneDiagnosticReceiver` 错当作 TV 端服务启动。
- 接入 Compose 浏览流程，覆盖首页、榜单、新剧、搜索、收藏、历史、影视详情、选集、播放操作、设备接力、诊断与设置。首批数据到达即可显示，Feed 采用去重/独立状态并按两屏目标补载；开始播放时取消 Feed 和在途封面加载。
- 为 Feed 分页和 TCP JSON 接力协议加入单元测试；迁移开心果已有 CENC、Catalog、弹幕、播放失败与 Session 测试。SessionTest 同时验证原有按 Tab 保存页面路径的 Router，因此继续迁移该纯状态路由模型，不搬入手机 UI；TV 主导航目前仍是独立的 TvTab/TvScreen，后续应统一 session 恢复与 TV 焦点/滚动恢复逻辑。

### 验证与未解决项
- Git 仓库审计、文件迁移及提交：已执行。
- 第一次 Actions 运行在 Android SDK setup 阶段失败，旧的 setup-android action 请求了已不再存在的 SDK 包 `tools`；随后发现 runner 的稳定 SDK channel 没有 `platforms;android-37`；将 compileSdk 降到 36 后，Gradle 依赖元数据检查又显示 14 个 AndroidX 依赖（Compose 1.12.1、Core 1.19.1、Lifecycle 2.11.0）要求 API37。为保留参考项目已使用的稳定库版本，CI 改为从 Android SDK 的 canary-inclusive channel 安装官方预览包名 `platforms;android-37.0` 与 Build Tools 37，同时 `targetSdk` 保持 36。Android 官方说明 Android 17 预览平台在 SDK Manager 中标为 Cinnamon Bun Preview，Build Tools 37 需选 37.x.x；API37 的平台包名和稳定 API 等级名不完全相同。此前失败发生在 Kotlin 编译前。
- Gradle 构建、依赖解析、Lint 与测试：最新工作流需重新运行确认，不在源码提交后预先宣称通过。
- `openhongguo` AAR 的 JNI/ABI：CI 会检查解析到的 AAR 候选和 APK 内原生库目录；仍须检查具体结果。
- x86_64 模拟器安装、ARM64 电视安装/解码、遥控器长时间焦点行为、实际播放流与接力跨设备通信：尚未通过真机/模拟器验证。
- 正式发布密钥暂未配置。Debug 测试构建与正式签名更新路径是两回事。
- 下一步重点：观察首轮 CI，修复任何构建/单测问题；验证 ABI；补全返回焦点/滚动位置恢复、后台预载调度与性能观察；真机测试接力和 CENC 播放；再创建真实的测试 Release 与 SHA-256。


## 2026-11-24：CI 故障可观测性与可选正式签名

- 第 12 次 Actions 的组合 `testDebugUnitTest lintDebug` 步骤长时间未给出可读日志/终态，因此不把该运行作为成功或失败依据；后续 workflow 将测试、Lint 拆开，避免两类任务互相遮蔽结果。
- 新 workflow 使用官方 Gradle setup action 缓存依赖；SDK/Wrapper 检查仍单独执行。
- 加入完整的 AAR JNI 条目盘点、三个目标 ABI 单独 APK 输出检查、Universal APK 验证、测试证书信息与 SHA-256 文件。
- CI 正式签名设计为可选但严格：四个 Secrets 全空时只出测试签名包；部分配置时报错；四项齐全时将 JKS 与 `signing-secrets.txt` 安全还原到仓库根目录，调用与本地一致的 Gradle release signing，验证证书并计算 SHA-256；临时文件始终在结束步骤清理，且不上传到 Artifact。
- 截止本日志条目，以上 workflow 变更已提交，必须以新一次 Actions 的最终结果验证。APK/Release/SHA 尚未宣称成功。


## 2026-11-24：Lint opt-in false positives

- 第 14 次 Actions 中单测全部通过；Lint 汇总为 96 errors、26 warnings、1 hint。首个错误是 `UnsafeOptInUsageError`，指向迁移自开心果且已使用文件级 `@file:OptIn(UnstableApi::class)` 的 Media3 源码。
- 开心果原工程本身已在其 `app/build.gradle.kts` 针对 `UnsafeOptInUsageError` 配置了 Lint disable；新版对齐这一已验证设置，不关闭整个 Lint，Kotlin 编译器仍会检查实际的 opt-in 使用。
- CI 的 Lint 步骤现在会在任务结束后打印完整文本报告，以便若仍有其他问题可以按 issue 和文件逐项修复。此次修复尚待下一轮 Actions 验证。


## 2026-11-24：首轮独立 Lint 与签名容错修正

- 第 14 次 Actions：`testDebugUnitTest` 已通过；`lintDebug` 因 96 个错误失败，第一项定位到迁移的 Media3 `ExoVideo.kt`。现有 Kotlin `@file:OptIn` 能满足编译器，但未被 Android Lint 当作 AndroidX 的 opt-in 证明，因此给实际使用 Media3 的类/函数补充 `androidx.annotation.OptIn(markerClass = [UnstableApi::class])`；后续 CI 将确认是否覆盖所有问题。
- 调整 Gradle 签名防护：只有显式请求 Release APK 打包、且根目录正式签名配置不完整时才失败；本地部分密钥配置不会再阻止 Debug 构建、测试与 Lint。签名属性文件解析错误会给出明确中文错误，不输出秘密值。
- 本次代码修正已提交；Lint、APK 打包、ABI 及 SHA 仍以新的 Actions 结果为准。


## 2026-11-24：Universal APK 的 ABI 验证方式修正

- 最新 Actions #16 的 `testDebugUnitTest` 通过，Android Lint 通过（0 errors，26 warnings，1 hint），`assembleDebug` 通过并生成 `app/build/outputs/apk/debug/app-universal-debug.apk`。
- ABI 检查脚本此前错误地要求三个独立 ABI 子目录 APK，导致脚本失败，未能进入 checksum/artifact 上传。Gradle 此次实际只产出 Universal APK，因此检查改为读取 APK ZIP 内的真实路径，要求三个 ABI 的 .so 文件均存在，并分别要求 openhongguo 的 `libgojni.so`。这比仅依据 split 配置或 AAR 的理论能力判断可靠。
- 参考的 `openhongguo:0.4.0` AAR 缓存中可见 `libgojni.so` 的 x86_64、arm64-v8a、armeabi-v7a、x86 构建；Lint 还警告 arm64-v8a 的 libgojni.so 未按 16 KB 页面对齐。ABI 是否装入 Universal APK 将由下一轮 CI 明确验证；16 KB 对齐警告保留记录，不伪装已解决。
- 因 ABI 检查失败，本轮未上传 APK artifact；需要通过新的 workflow 后才能发布 SHA-256 和下载链接。

 
## 2026-11-24：修复 ABI 校验脚本 YAML 截断

- 提交 #17 在 Actions 中立即失败且没有 job/check-run；检查发现上一轮生成 workflow 时，ABI 断言行在 `/libgojni.so` 处被截断，造成无效 YAML。这不是应用源码编译失败。
- 已恢复完整的 grep 断言、for 循环收尾及 success 日志。下一轮 Actions 需验证 workflow 被正确解析，并实际检查 Universal APK 内的三种 ABI。

 
## 2026-11-24：恢复已知可解析的 Actions 工作流基线

- 新的 ABI 检查修订 #17/#18 以零 job、零 check-run 在创建后立即失败，表明 Workflow 在 runner 分配前未被正常加载。为避免继续局部编辑一个可能已损坏的 YAML 文件，恢复到已被 #16 实际执行、单测/Lint/assembleDebug 都成功的 workflow 基线，然后只替换 ABI 检查区块。
- 新块直接检查 Universal APK 的 ZIP 条目，验证三个 ABI 的本地库和每个 ABI 的 `libgojni.so`；其余测试、Lint、签名和 Artifact 步骤沿用已执行过的工作流。
- 下一次 CI 的首要验证是 workflow 正常生成 job；仍需确认 ABI 检查与 Artifact 上传成功。


## 2026-11-24：按项目要求在仓库根目录补齐实际签名文件

- 已在新版仓库根目录生成并加入独立的 `hongguoTV-next-release.jks` 与 `signing-secrets.txt`，不再只提供 Gradle 读取逻辑或要求用户另行创建。
- JKS 是新生成的独立 RSA 发布证书（alias `hongguotv-next-release`），不复制开心果或旧 TV 的密钥。
- GitHub Actions 签名步骤优先使用这两个根目录文件；只有两者确实不存在时才退回 Actions Secrets 流程。自用项目的正式签名构建不再依赖额外配置 Secrets。
- 工作流上传 APK、校验文件与证书信息，不将 JKS/属性文件作为 Artifact；旧的两个参考仓库未修改。
- 正式签名首个 CI 构建与最终 SHA-256 需要由新一次 Actions 确认。


## 2026-11-24：根目录正式签名与 CI 交付验证完成

- 按要求把独立的 `hongguoTV-next-release.jks` 和 `signing-secrets.txt` 实际放到了仓库根目录；Gradle 从 `rootProject.file(...)` 读取。CI 优先使用根目录这两份文件，不再要求配置 Actions Secrets。该提交为 `9317ca40ae8c08dae533f5ccb6c3a4e7379eec46`。
- Actions #20：单元测试成功；Android Lint 成功（保留 26 条 warning 和 1 条 hint，没有 error）；Debug APK 成功构建；Universal APK 实际包含 `x86_64`、`arm64-v8a`、`armeabi-v7a` 三种 native library，三种 ABI 均发现 `libgojni.so`。
- 正式签名 Release 构建、`apksigner verify` 和证书/SHA 产物生成均成功；签名证书 SHA-256 指纹：`ebf27c606826ad1ae5ac72a2ae31751b2769c2c0029b3ac4d1e748d3daa57a34`。
- 正式签名 APK：`hongguoTV-next-v0.1.0-universal-release-signed.apk`；SHA-256：`11145b0b612765dfaef40b57e191d54a63423071058c8a393c6b43e22b3c9dbb`。SHA 在 runner 和下载到本地后均已校验通过。
- 构建记录：[Actions #20](https://github.com/xiaobei206/hongguoTV-next/actions/runs/45346930533)。正式签名 artifact 名称：`hongguoTV-next-v0.1.0-formal-release`；测试签名 artifact 名称：`hongguoTV-next-v0.1.0-test-apk`。
- 仍未通过真机验证：电视实机安装/播放、遥控器焦点行为、实际手机到电视的接力通信。Lint 还提示 `openhongguo 0.4.0` 的 ARM64 `libgojni.so` 未满足 16 KB 页面对齐；当前没有声称解决该项。


## 2026-11-24：v0.1.4 首轮 API 23 Lint 失败与兼容修复

- 首轮 Actions 的 SDK AAR 构建、单元测试通过；Android Lint 发现 14 个 NewApi 错误，集中在三个位置：CoverStore 无条件使用 API 28 ImageDecoder；Store 使用 API 26 java.nio.file.Files.move；TvDiscovery 使用 API 24 ConcurrentHashMap.KeySetView.clear。
- CoverStore 改为按系统版本选择解码器：API 28+ 保留 ImageDecoder 与现有缩放逻辑；API 23-27 使用 BitmapFactory bounds + inSampleSize + 600px 限制，并对下载请求优先声明 JPEG/PNG/WebP。旧系统不支持 HEIC 时允许封面解码失败，但不影响核心浏览/播放路径。
- JSONFile 写入先写同目录临时文件并 fsync，再用 File.renameTo 在同一文件系统原子替换目标文件；此 API 在旧 Android 可用，也能在 JVM 单测环境真实执行。
- TvDiscovery 的 resolving 集合改为 Collections.synchronizedSet，规避 API 24 KeySetView.clear 限制；其他发现协议和并发设备映射保持不变。
- 修复后需重新运行完整 Android CI，特别确认 API 23 Manifest、Lint、Universal ABI、Debug/正式签名 Release 与 SHA-256。


## 2026-11-24：修正 API 23 文件替换实现的 JVM 测试兼容

- 第二轮 Actions 的 SDK AAR 构建通过，但 4 个历史/session 持久化单测失败。根因是测试环境启用了 unitTests.isReturnDefaultValues，android.system.Os 被 Android 单测替身替换，Os.rename 没有执行文件系统操作。
- JSONFile 改为对同目录临时文件调用 File.renameTo，并检查布尔返回值；Android/Linux 同文件系统 rename 会原子替换现有目标，File API 从早期 Android 即可使用，且 JVM 单测可实际验证读写。
- 保留临时文件写入、fd.sync 和失败时清理逻辑；不通过关闭测试或跳过持久化断言规避问题。
- 下一轮 CI 先验证 48 项单测，再执行 API 23 Lint、APK Manifest、ABI、签名与 SHA-256。


## 2026-11-24：v0.1.4 发布工作流准备

- v0.1.4 的兼容代码已通过一轮完整 Android CI：SDK AAR、48 项应用单元测试、Android Lint、API 23 APK Manifest 门禁、x86_64/arm64-v8a/armeabi-v7a ABI 检查、测试 APK、正式签名 APK、证书与 SHA-256 校验均成功。
- 新增独立 `.github/workflows/publish-v0.1.4.yml`，只会在带 `release: prepare v0.1.4 final` 标记且该提交仍是 main 最新提交时，下载本次成功 CI 的正式签名 Artifact，重新验证 SHA 与证书后发布 v0.1.4。
- 最终标记提交会再次触发完整 Android CI；只有该次 CI 全部成功，Release 才会自动发布。真实电视安装与播放仍须用户在设备上验收。


## 2026-11-25：v0.1.7 播放诊断图形与 VLC 管道稳定性

- Stats for nerds 原有网络活动图使用当前窗口最大流量动态归一化，突发流量会压低其余柱形。改为固定 20 秒 / 40 个 500ms 槽位、固定参考刻度、平方根高度映射、分档颜色与动态扫描线；诊断文字和采集入口保持不变。
- Android CI 对应的自维护 SDK 补丁修复 VLC 管道完成竞态：播放器读到 EOF 后，给 FFmpeg Wait 结果最多 500ms 的发布窗口，避免 channel goroutine 调度稍晚而误判 ErrStopped；真实提前关闭仍会取消 FFmpeg 并按停止处理。
- VLC 管道回归测试连续执行 25 次，并检查播放器实际收到完整输出及 FFmpeg 参数。
- 先前 v0.1.6 正式 Release 指向提交 `115eff6a260220f9b4c101bd2a713aa3f68b0c50`，不包含本次图形与 SDK 修复；本次按递增版本发布为 v0.1.7 / versionCode 8，不覆盖旧 Release。
- v0.1.7 发布工作流只接受提交信息 `release: prepare v0.1.7 final`，并要求该提交仍是 main 最新提交；只有该提交对应的 Android CI 全部通过，才会上传正式签名 APK、SHA-256 与证书信息并创建 GitHub Release。
- v0.1.6 的完整 CI 已通过；v0.1.7 的最终验证以带 release marker 的新一轮 Android CI 和发布工作流结果为准。


## 2026-11-25：v0.1.8 按开心果完整移植三条诊断图

- 复核后确认 v0.1.7 的诊断面板只画了 Network Activity，遗漏开心果原有的 Connection Speed 和 Buffer Health 两图，且文字排版未沿用参考实现；这是移植不完整，不应称为已完成优化。
- v0.1.8 改为沿用开心果的三行图结构、统一 9sp 标签/数值、固定 40 个 500ms 槽位、同一暗色底板与扫描游标；速度曲线使用蓝色平滑折线，网络活动使用分档柱形，缓冲健康度使用按缓冲秒数分档的填充曲线。
- 三图均使用真实诊断采样：连接速度来自 Media3/网络诊断速度估算，网络活动来自实际接收字节，缓冲健康度来自 ExoPlayer 当前缓冲位置。CI 与电视实机观感仍需分别验收。
