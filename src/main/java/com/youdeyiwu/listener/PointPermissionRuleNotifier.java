package com.youdeyiwu.listener;

import static com.youdeyiwu.constant.PointConstant.POINT_REWARD_BY_SYSTEM;
import static com.youdeyiwu.tool.Tool.getDifferenceSign;

import com.youdeyiwu.constant.PointConfigConstant;
import com.youdeyiwu.enums.config.ConfigTypeEnum;
import com.youdeyiwu.enums.point.SignEnum;
import com.youdeyiwu.event.MessageApplicationEvent;
import com.youdeyiwu.event.PointPermissionRuleApplicationEvent;
import com.youdeyiwu.exception.CustomException;
import com.youdeyiwu.exception.UserNotFoundException;
import com.youdeyiwu.model.dto.point.PointPermissionRuleEventDto;
import com.youdeyiwu.model.dto.point.UpdatePointDto;
import com.youdeyiwu.model.entity.message.MessageEntity;
import com.youdeyiwu.model.entity.point.PointEntity;
import com.youdeyiwu.model.entity.point.PointHistoryEntity;
import com.youdeyiwu.model.entity.point.PointPermissionRuleEntity;
import com.youdeyiwu.model.entity.user.UserEntity;
import com.youdeyiwu.repository.config.ConfigRepository;
import com.youdeyiwu.repository.point.PointPermissionRuleRepository;
import com.youdeyiwu.repository.user.UserRepository;
import com.youdeyiwu.security.SecurityService;
import com.youdeyiwu.service.point.PointCoreService;
import com.youdeyiwu.service.point.PointService;
import com.youdeyiwu.tool.I18nTool;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * post permission rule listener.
 *
 * @author dafengzhen
 */
@RequiredArgsConstructor
@Component
public class PointPermissionRuleNotifier
    implements ApplicationListener<PointPermissionRuleApplicationEvent> {

  private final PointPermissionRuleRepository pointPermissionRuleRepository;

  private final ConfigRepository configRepository;

  private final SecurityService securityService;

  private final PointService pointService;

  private final PointCoreService pointCoreService;

  private final I18nTool i18nTool;

  private final ApplicationEventPublisher publisher;

  private final UserRepository userRepository;

  @Override
  public void onApplicationEvent(PointPermissionRuleApplicationEvent event) {
    Boolean enable = configRepository.findOptionalByTypeAndName(
            ConfigTypeEnum.POINT,
            PointConfigConstant.ENABLE
        )
        .map(configEntity -> Boolean.valueOf(configEntity.getValue()))
        .orElse(false);

    if (Boolean.FALSE.equals(enable)) {
      return;
    }

    PointPermissionRuleEventDto dto = (PointPermissionRuleEventDto) event.getSource();
    Optional<PointPermissionRuleEntity> byPermissionRuleName =
        pointPermissionRuleRepository.findByPermissionRuleName(dto.permissionRuleName());
    if (byPermissionRuleName.isEmpty() || Boolean.FALSE.equals(byPermissionRuleName.get().getEnable())) {
      return;
    }

    PointPermissionRuleEntity pointPermissionRuleEntity = byPermissionRuleName.get();
    Integer requiredPoints = pointPermissionRuleEntity.getRequiredPoints();
    if (requiredPoints <= 0) {
      return;
    }

    if (securityService.isAnonymous()) {
      throw new CustomException(i18nTool.getMessage("point.permissionRule.anonymous"));
    }

    handleActions(pointPermissionRuleEntity, dto);
  }

  /**
   * handle actions.
   *
   * @param pointPermissionRuleEntity pointPermissionRuleEntity
   * @param dto                       dto
   */
  private void handleActions(
      PointPermissionRuleEntity pointPermissionRuleEntity,
      PointPermissionRuleEventDto dto
  ) {
    PointEntity pointEntity = pointService.findPointByUserId(securityService.getUserId());
    int points = Math.abs(pointEntity.getPoints());
    int requiredPoints = Math.abs(pointPermissionRuleEntity.getRequiredPoints());
    if (points < requiredPoints) {
      throw new CustomException(
          i18nTool.getMessage(
              "point.permissionRule.points",
              Map.of(
                  "requiredPoints", requiredPoints,
                  "points", points
              )
          )
      );
    }

    pointCoreService.update(
        pointEntity,
        new UpdatePointDto(
            -Math.abs(pointPermissionRuleEntity.getOperationCost()),
            null,
            null
        )
    );

    getDifferenceSign(
        pointEntity,
        (flag, difference) -> {
          String source = Objects.isNull(dto.from()) ? i18nTool.getMessage("point.systemService") : dto.from();
          PointHistoryEntity pointHistoryEntity = new PointHistoryEntity();
          pointHistoryEntity.setPointValue(difference);
          pointHistoryEntity.setSign(flag);
          pointHistoryEntity.setPermissionRuleName(dto.permissionRuleName());
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
              userRepository.findById(securityService.getUserId()).orElseThrow(UserNotFoundException::new)
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
