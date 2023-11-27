# Overview
本项目的特色在于将基于深度学习和计算机视觉的实时人体姿态估计技术应用于医疗服务场景，具体而言本项目具有以下创新特点：1.采用增强学习能力的轻量化主干网络 CSPNeXt 和优化的关节点定位算法，为实时人体姿态估计提供高效的算法支持。2. 设计了满足特定医疗康复指导需求的姿态智能匹配算法，将医疗服务的需要与技术的实现紧密结合起来，为患者提供高质量的康复训练指导。3. 由于实时人体姿态估计任务需要处理患者的生理数据，为了保护患者的隐私，本项目设计了本地推理的策略，将数据处理和算法运行都放在本地设备上，不需要将数据传输至云端，可以有效保护患者个人信息，提高数据安全性。

# APP `s body
1. UI:
    1. 将登录和登出功能的界面设计进行优化，确保用户友好的交互体验。
    2. 使用单一的数据库连接池来管理数据库连接，以提高效率和性能。
    3. 设计精美、直观的用户界面，符合现代设计准则。
    4. 在主界面中清晰传达核心理念，突出展示您的标志/徽标。
    5. 创建易于使用的界面，允许用户调整和拖动视频小窗口，以提供更好的用户体验。
2. 姿态估计功能:
    1. 对输入的视频帧或相机捕获的帧进行归一化处理，确保数据的一致性。
    2. 优化关键点定位和展示算法，以实现实时性能的提升。
    3. 实现数据记录和妥善保存关节点数据，以备将来使用。
3. 医疗指导:
    1. 实现视频观看功能，使用户能够轻松观看相关内容。
    2. 实现规范化的视频动作检测，以便对用户的动作进行评估和指导。
    3. 设计高效的算法，用于比较之前姿态估计功能获得的关节点数据与当前用户的动作数据。为了辅助矫正，可以实现关键词提醒，引导用户执行正确的动作。

# Preview 
软件截图、视频等

# 待补充

# Citations 
感谢 Openmmlab 社区的 RTMPose 模型 [mmpose/projects/rtmpose at main · open-mmlab/mmpose (github.com)](https://github.com/open-mmlab/mmpose/tree/main/projects/rtmpose) 以及 OpenCV 等开源库的支持。

