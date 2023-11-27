# FlingScrollView
Android自定义 View惯性滚动效果（不使用Scroller）

## 前言：
* 看了网上很多惯性滚动方案，都是通过Scroller 配合 computeScroll实现的，但在实际开发中可能有一些场景不合适，比如协调布局，内部子View有特别复杂的联动效果，需要通过偏移来配合。
  原理很简单：使用VelocityTracker（速度跟踪器），从UP事件拿到初始速度**逐步递减，直至停止**的过程。
  我写了两个版本：**粗糙版本** 和 **RecyclerView版本**，区别在于，前者是逐步递减过程是我自己算的，后者是我将RecyclerView里面惯性运动相关的代码，扒了出来进行封装，**推荐使用后者**。

## 粗糙版本


## RecyclerView版本




