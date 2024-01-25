'use client';

import { useMutation } from '@tanstack/react-query';
import { type ChangeEvent, type FormEvent, useContext, useState } from 'react';
import { GlobalContext } from '@/app/contexts';
import Box from '@/app/admin/common/box';
import { trimObjectStrings } from '@/app/common/client';
import NotApprovedPostReviewQueuesAction from '@/app/actions/posts/review-queues/not-approved-post-review-queues-action';

export default function NotApproved({ id }: { id: number }) {
  const { toast } = useContext(GlobalContext);
  const [form, setForm] = useState<{
    reason: string;
  }>({
    reason: '',
  });

  const notApprovedPostReviewQueuesActionMutation = useMutation({
    mutationFn: NotApprovedPostReviewQueuesAction,
  });

  async function onClickButton() {
    try {
      const variables = trimObjectStrings(form);
      await notApprovedPostReviewQueuesActionMutation.mutateAsync({
        id,
        variables,
      });

      toast.current.show({
        type: 'success',
        message: 'The post has been successfully rejected',
      });
    } catch (e: any) {
      notApprovedPostReviewQueuesActionMutation.reset();
      toast.current.show({
        type: 'danger',
        message: e.message,
      });
    }
  }

  function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.stopPropagation();
    e.preventDefault();
  }

  function onChangeForm(
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) {
    const name = e.target.name;
    const value = e.target.value;
    setForm({ ...form, [name]: value });
  }

  return (
    <Box>
      <form className="vstack gap-4" onSubmit={onSubmit}>
        <div>
          <label className="form-label">Reason for rejection</label>
          <textarea
            rows={3}
            disabled={notApprovedPostReviewQueuesActionMutation.isSuccess}
            className="form-control"
            name="reason"
            value={form.reason}
            onChange={onChangeForm}
            placeholder="Please enter the reason"
            aria-describedby="reason"
          />
          <div className="form-text">
            Optional. However, we still recommend that you inform the person of
            the reasons for the rejection, so that they can make targeted
            corrections or have a clear understanding of any requirements. We
            believe that doing so would be beneficial
          </div>
        </div>

        <div>
          <button
            onClick={onClickButton}
            disabled={
              notApprovedPostReviewQueuesActionMutation.isPending ||
              notApprovedPostReviewQueuesActionMutation.isSuccess
            }
            type="button"
            className="btn btn-success"
          >
            {notApprovedPostReviewQueuesActionMutation.isPending
              ? 'Processing'
              : 'Cancel Reception'}
          </button>
        </div>
      </form>
    </Box>
  );
}
