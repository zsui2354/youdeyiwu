'use client';

import Box from '@/app/[locale]/admin/common/box';
import Nodata from '@/app/[locale]/common/nodata';
import {
  type IPointRule,
  RuleNameEnum,
} from '@/app/[locale]/interfaces/points';
import { useContext, useState } from 'react';
import clsx from 'clsx';
import { GlobalContext } from '@/app/[locale]/contexts';
import { useMutation } from '@tanstack/react-query';
import SaveRulesPointsAction, {
  type ISaveRulesPointsActionVariables,
} from '@/app/[locale]/actions/points/rules/save-rules-points-action';
import useMenuActionPermission from '@/app/[locale]/hooks/useMenuActionPermission';

const tips = {
  LIKE_POST: 'earning points for liking a post',
  LIKE_COMMENT: 'earning points for liking a comment',
  LIKE_REPLY: 'earning points for liking a reply',
  COMMENT_POST: 'earning points for commenting on a post',
  REPLY_POST: 'earning points for replying to a post',
  FOLLOW_POST: 'earning points for following a post',
  FAVORITE_POST: 'earning points for marking a post as a favorite',
  DISLIKE_POST: 'earning points for disliking a post',
  DISLIKE_COMMENT: 'earning points for disliking a comment',
  DISLIKE_REPLY: 'earning points for disliking a reply',
  POST_APPROVED: 'earning points for having a post approved',
  POST_NOT_APPROVED: 'earning points for having a post not approved',
  POST_PENDING_REVIEW: 'earning points for a post pending review',
  VISIT_POST: 'earning points for visiting a post',
  CREATE_POST: 'earning points for creating a new post',
};

const rules = Object.keys(RuleNameEnum).map((item) => {
  return {
    ruleName: item,
    initiatorRewardPoints: 0,
    receiverRewardPoints: 0,
  };
}) as IPointRule[];

export default function PointRules({ data }: { data: IPointRule[] }) {
  const { toast } = useContext(GlobalContext);
  const [content, setContent] = useState<IPointRule[]>(
    rules.map((item, index) => {
      const find = data.find((_item) => _item.ruleName === item.ruleName);
      return find
        ? { ...find, _tip: tips[item.ruleName] }
        : { ...item, id: index, _tip: tips[item.ruleName] };
    }),
  );
  const [isUpdate, setIsUpdate] = useState(false);
  const [saving, setSaving] = useState(false);
  const { isActionDisabled, AccessDeniedAlert } = useMenuActionPermission(
    '/admin/points/rules',
    'Point Rules#Update',
  );

  const saveRulesPointsActionMutation = useMutation({
    mutationFn: async (variables: ISaveRulesPointsActionVariables) => {
      const response = await SaveRulesPointsAction(variables);
      if (response.isError) {
        throw response;
      }
    },
  });

  function onClickUpdate() {
    setIsUpdate(!isUpdate);
  }

  async function onClickSave() {
    try {
      if (saving) {
        return;
      }
      setSaving(true);

      const _content = content.map((item) => ({
        ruleName: item.ruleName,
        initiatorRewardPoints: item.initiatorRewardPoints,
        receiverRewardPoints: item.receiverRewardPoints,
      }));

      for (let item of _content) {
        await saveRulesPointsActionMutation.mutateAsync(item);
      }

      setIsUpdate(false);
      toast.current.show({
        type: 'success',
        message: 'Successfully updated',
      });
    } catch (e: any) {
      saveRulesPointsActionMutation.reset();
      toast.current.show({
        type: 'danger',
        message: e.message,
      });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Box
      header={
        <div className="d-flex align-items-center justify-content-between gap-4">
          <div></div>
          <div className="d-flex gap-2">
            <div>
              <button
                disabled={saving}
                onClick={onClickUpdate}
                type="button"
                className={clsx(
                  'btn btn-sm',
                  isUpdate ? 'btn-secondary' : 'btn-primary',
                )}
              >
                {isUpdate ? 'Cancel Update' : 'Update'}
              </button>
            </div>

            {isUpdate && (
              <div className="d-flex flex-column">
                <button
                  disabled={isActionDisabled || saving}
                  onClick={onClickSave}
                  type="button"
                  className="btn btn-sm btn-success"
                >
                  {saving ? 'Saving' : 'Save'}
                </button>
                <AccessDeniedAlert />
              </div>
            )}
          </div>
        </div>
      }
    >
      <div className="table-responsive">
        <table className="table align-middle table-striped">
          <caption>
            The senders or receivers can be rewarded with points, with a default
            value of 0. In most cases, the value should be a positive number
          </caption>
          <thead>
            <tr>
              <th scope="col">Rule</th>
              <th scope="col">InitiatorRewardPoints</th>
              <th scope="col">ReceiverRewardPoints</th>
            </tr>
          </thead>
          <tbody>
            {content.map((item) => {
              return (
                <tr key={item.id}>
                  <th scope="row">{item._tip}</th>
                  <td>
                    {isUpdate ? (
                      <input
                        required
                        disabled={saving}
                        type="number"
                        className="form-control"
                        name="initiatorRewardPoints"
                        value={item.initiatorRewardPoints}
                        onChange={(event) => {
                          const find = content.find(
                            (_item) => item.id === _item.id,
                          );
                          if (!find) {
                            return;
                          }

                          const value = parseInt(event.target.value);
                          if (isNaN(value)) {
                            return;
                          }

                          find.initiatorRewardPoints = value;
                          setContent([...content]);
                        }}
                        placeholder="The default value is 0, and the value should be a positive number"
                        aria-describedby="initiatorRewardPoints"
                      />
                    ) : (
                      <>{item.initiatorRewardPoints}</>
                    )}
                  </td>
                  <td>
                    {isUpdate ? (
                      <input
                        required
                        disabled={saving}
                        type="number"
                        className="form-control"
                        name="receiverRewardPoints"
                        value={item.receiverRewardPoints}
                        onChange={(event) => {
                          const find = content.find(
                            (_item) => item.id === _item.id,
                          );
                          if (!find) {
                            return;
                          }

                          const value = parseInt(event.target.value);
                          if (isNaN(value)) {
                            return;
                          }

                          find.receiverRewardPoints = value;
                          setContent([...content]);
                        }}
                        placeholder="The default value is 0, and the value should be a positive number"
                        aria-describedby="receiverRewardPoints"
                      />
                    ) : (
                      <>{item.receiverRewardPoints}</>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {content.length === 0 && <Nodata />}
    </Box>
  );
}
