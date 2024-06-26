package com.youdeyiwu.listener;

import static com.youdeyiwu.constant.PointConstant.POINT_REWARD_BY_SYSTEM;
import static com.youdeyiwu.tool.Tool.calculatePoints;
import static com.youdeyiwu.tool.Tool.getDifferenceSign;

import com.youdeyiwu.constant.PointConfigConstant;
import com.youdeyiwu.enums.config.ConfigTypeEnum;
import com.youdeyiwu.enums.point.SignEnum;
import com.youdeyiwu.event.MessageApplicationEvent;
import com.youdeyiwu.event.PointRuleApplicationEvent;
import com.youdeyiwu.exception.UserNotFoundException;
import com.youdeyiwu.model.dto.point.PointRuleEventDto;
import com.youdeyiwu.model.dto.point.UpdatePointDto;
import com.youdeyiwu.model.entity.message.MessageEntity;
import com.youdeyiwu.model.entity.point.PointEntity;
import com.youdeyiwu.model.entity.point.PointHistoryEntity;
import com.youdeyiwu.model.entity.point.PointRuleEntity;
import com.youdeyiwu.model.entity.user.UserEntity;
import com.youdeyiwu.repository.config.ConfigRepository;
import com.youdeyiwu.repository.point.PointHistoryRepository;
import com.youdeyiwu.repository.point.PointRuleRepository;
import com.youdeyiwu.repository.user.UserRepository;
import com.youdeyiwu.security.SecurityService;
import com.youdeyiwu.service.point.PointCoreService;
import com.youdeyiwu.service.point.PointService;
import com.youdeyiwu.tool.I18nTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * post rule listener.
 *
 * @author dafengzhen
 */
@RequiredArgsConstructor
@Component
public class PointRuleNotifier
    implements ApplicationListener<PointRuleApplicationEvent> {

  private final PointRuleRepository pointRuleRepository;

  private final PointHistoryRepository pointHistoryRepository;

  private final UserRepository userRepository;

  private final ConfigRepository configRepository;

  private final SecurityService securityService;

  private final PointService pointService;

  private final PointCoreService pointCoreService;

  private final ApplicationEventPublisher publisher;

  private final I18nTool i18nTool;

  @Override
  public void onApplicationEvent(PointRuleApplicationEvent event) {
    Boolean enable = configRepository.findOptionalByTypeAndName(
            ConfigTypeEnum.POINT,
            PointConfigConstant.ENABLE
        )
        .map(configEntity -> Boolean.valueOf(configEntity.getValue()))
        .orElse(false);

    if (Boolean.FALSE.equals(enable)) {
      return;
    }

    PointRuleEventDto dto = (PointRuleEventDto) event.getSource();
    Optional<PointRuleEntity> byRuleName = pointRuleRepository.findByRuleName(dto.ruleName());
    if (byRuleName.isEmpty() || Boolean.FALSE.equals(byRuleName.get().getEnable())) {
      return;
    }

    handleActions(dto, byRuleName.get());
  }

  /**
   * handle actions.
   *
   * @param dto             dto
   * @param pointRuleEntity pointRuleEntity
   */
  private void handleActions(PointRuleEventDto dto, PointRuleEntity pointRuleEntity) {
    List<Long> userIds = new ArrayList<>();
    Integer initiatorRewardPoints = null;
    Integer receiverRewardPoints = null;

    if (securityService.isAuthenticated()) {
      userIds.add(securityService.getUserId());
      initiatorRewardPoints = pointRuleEntity.getInitiatorRewardPoints();
    }

    if (!CollectionUtils.isEmpty(dto.receivedUserIds())) {
      userIds.addAll(dto.receivedUserIds());
      receiverRewardPoints = pointRuleEntity.getReceiverRewardPoints();
    }

    if (Objects.isNull(initiatorRewardPoints) && Objects.isNull(receiverRewardPoints)) {
      return;
    }

    final Integer finalInitiatorRewardPoints = initiatorRewardPoints;
    final Integer finalReceiverRewardPoints = receiverRewardPoints;
    userIds.stream().findFirst().ifPresent(userId -> {
      updatePointsAndSendMessage(
          userRepository.findById(userId).orElseThrow(UserNotFoundException::new),
          finalInitiatorRewardPoints,
          dto
      );
    });
    userIds.stream().skip(1).forEach(userId -> {
      updatePointsAndSendMessage(
          userRepository.findById(userId).orElseThrow(UserNotFoundException::new),
          finalReceiverRewardPoints,
          dto
      );
    });
  }

  /**
   * Update points and send messages for a user.
   *
   * @param user         user
   * @param rewardPoints rewardPoints
   * @param dto          dto
   */
  private void updatePointsAndSendMessage(
      UserEntity user,
      Integer rewardPoints, PointRuleEventDto dto
  ) {
    SignEnum sign = dto.sign();
    if (Boolean.TRUE.equals(dto.checkHistoryPoints())) {
      sign = pointHistoryRepository.findLatestPointsHistoryByUserIdAndRuleName(
              user.getId(),
              dto.ruleName()
          )
          .map(pointHistoryEntity -> switch (pointHistoryEntity.getSign()) {
            case POSITIVE -> SignEnum.NEGATIVE;
            case NEGATIVE -> SignEnum.POSITIVE;
            case ZERO -> dto.sign();
          })
          .orElseGet(dto::sign);
    }

    PointEntity pointEntity = pointCoreService.update(
        pointService.findPointByUserEntity(user),
        new UpdatePointDto(calculatePoints(rewardPoints, sign), null, null)
    );

    getDifferenceSign(
        pointEntity,
        (flag, difference) -> {
          String source = Objects.isNull(dto.from()) ? i18nTool.getMessage("point.systemService") : dto.from();
          PointHistoryEntity pointHistoryEntity = new PointHistoryEntity();
          pointHistoryEntity.setPointValue(difference);
          pointHistoryEntity.setSign(flag);
          pointHistoryEntity.setRuleName(dto.ruleName());
          pointHistoryEntity.setReason(POINT_REWARD_BY_SYSTEM);
          pointHistoryEntity.setSource(source);
          pointHistoryEntity.setSourceLink(dto.link());
          pointCoreService.create(pointEntity, pointHistoryEntity);
          sendMessage(
              Map.of(
                  "increased", flag == SignEnum.POSITIVE ? difference : 0,
                  "decreased", flag == SignEnum.NEGATIVE ? difference : 0,
                  "remaining", pointEntity.getPoints(),
                  "source", source
              ),
              dto.link(),
              user
          );
        }
    );
  }

  /**
   * send message.
   *
   * @param overviewArgs overviewArgs
   * @param link         link
   * @param receiver     receiver
   */
  private void sendMessage(
      Map<String, Object> overviewArgs,
      String link,
      UserEntity receiver
  ) {
    if (Objects.isNull(overviewArgs)) {
      return;
    }

    MessageEntity messageEntity = new MessageEntity();
    messageEntity.setName(i18nTool.getMessage("point.pointRule.message.name"));
    messageEntity.setOverview(i18nTool.getMessage("point.pointRule.message.overview", overviewArgs));
    messageEntity.setLink(link);
    messageEntity.setReceiver(receiver);
    publisher.publishEvent(new MessageApplicationEvent(messageEntity));
  }
}
