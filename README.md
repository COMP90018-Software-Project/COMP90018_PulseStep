# COMP90018_PulseStep 

### 主要功能
1. **位置追踪和地图展示**
   - 使用 Google Maps API 实时追踪用户的位置，并在地图上显示当前位置。
   - 每3秒更新一次位置 (可以更快,但是我怕我的额度用光了, 而且快了也不一定准, 定位会漂移)

2. **路径绘制**  
   - 距离小于 1 米或步数未增加时，将不会更新路径，以避免由于 GPS 漂移导致的误差。
   - 在第一次获得位置时，不立即绘制，而是等待下一个有效点才开始绘制，确保路径的起始点准确。
   - 因为初始定位时 GPS 信号通常不稳，可能会产生较大的漂移。通过跳过初始点的计算，可以减少初始漂移对配速计算的影响。
  
3. **步数统计**
   - 通过 `TYPE_STEP_DETECTOR` 来统计用户的步数。
   - 步数统计功能需要 "ACTIVITY_RECOGNITION" 权限。若权限被拒绝，则无法启用步数统计。
   - 步数大于零时，才开始路径绘制，以确保用户确实在移动而不是在静止。

4. **定时器与配速计算**
   - 使用 `Handler` 和 `Runnable` 实现了一个计时器，通过 `SystemClock` 记录开始时间和暂停时间。
   - 通过配速计算显示用户的平均配速，当配速不合理时（过快或过慢），会忽略该数据以避免误差。
   - 配速 **低于 1 分钟/公里**：这可能是由于 GPS 信号误差或异常数据引起的非正常速度。代码将其视为异常值并忽略，不会显示该配速。
   - 配速 **高于 30 分钟/公里**：这通常意味着速度非常慢，甚至可能是由于用户静止不动引起的。为了确保数据的合理性，这种情况也会被忽略。
   - 任何超出这一合理范围的配速都不会被显示，而是使用默认值来代替，并在代码中记录日志，方便后续调试和排查。

5. **UI交互**
   - 提供了暂停和恢复按钮，通过点击可控制路径追踪的暂停和恢复状态。
   - 提供了显示最后轨迹的功能按钮，点击后可以展示用户的最后一次运动轨迹。

6. **卡路里计算**
   - 基于公式近似计算： 卡路里消耗 =·= 用户体重（公斤）× 运动时间（分钟）× 燃烧系数 (跳绳约0.14-0.2）。
   - 跳绳假设了用户在计时的全程中平均地进行运动。
   - 跑步的卡路里消耗的权重基于配速
  
7. **跳绳计数**
   - <del>基于每个时间片段内运动加速度的变化曲线简单计算得出，</del>计算精度可能会受设备影响（误计漏计等），在后台运行时精度会有一定程度的下降。
   - <del>认为速度曲线近似正弦函数，因此加速度曲线类似余弦函数。计数器通过在每个近似0值时计数来为跳绳计次</del>
   - 考虑到跳绳是对抗重力的运动，以及手机握持的方向以及在运动中未知的方向不一定一定，因此使用实时更新的重力方向上的加速度曲线来为跳绳计次。
   - 建议手持或固定在手臂上以达到更好的测量效果。
  
### 参数设置
When implementing GPS tracking, finding the right balance between update frequency and battery consumption is key. For **walking** or slower activities, a **5 to 10-second** interval is often recommended to ensure accurate tracking without excessive power drain. A distance threshold of **5 to 10 meters** can also trigger updates efficiently[^1][^2]. For **driving**, shorter intervals like **1 to 5 seconds** may be more appropriate, especially if high precision is needed. In background mode, **30 seconds to 1 minute** updates can be used to save battery while maintaining adequate performance[^1][^3]. Using the **Fused Location Provider** can help balance accuracy and power consumption by combining data from multiple sources[^4].


[^1]: [Unlocking Android Background Location Updates](19)
[^2]: [BrickHouse Security - GPS Tracking Intervals](22)
[^3]: [Suunto - How to Get More Accurate GPS Tracking](20)
[^4]: [Android Developers - Request Location Updates](21)
