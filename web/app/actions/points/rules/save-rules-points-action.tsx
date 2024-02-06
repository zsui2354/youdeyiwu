'use server';

import { type IError } from '@/app/interfaces';
import FetchDataException from '@/app/exception/fetch-data-exception';
import { AUTHENTICATION_HEADER, JSON_HEADER, POST } from '@/app/constants';
import { revalidateTag } from 'next/cache';
import { checkResponseStatus } from '@/app/common/server';
import { RuleNameEnum } from '@/app/interfaces/points';

export interface ISaveRulesPointsActionVariables {
  ruleName: RuleNameEnum;
  initiatorRewardPoints?: number;
  receiverRewardPoints?: number;
}

export default async function SaveRulesPointsAction(
  variables: ISaveRulesPointsActionVariables,
) {
  const response = await fetch(process.env.API_SERVER + '/points/rules', {
    method: POST,
    headers: {
      ...AUTHENTICATION_HEADER(),
      ...JSON_HEADER,
    },
    body: JSON.stringify(variables),
  });

  if (!response.ok) {
    const data = (await response.json()) as IError;
    checkResponseStatus(response.status);
    throw FetchDataException(data.message);
  }

  revalidateTag('/admin/points/rules');
}