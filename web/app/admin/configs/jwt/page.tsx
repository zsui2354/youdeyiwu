import { type Metadata } from 'next';
import JwtConfig from '@/app/admin/configs/jwt/jwt-config';
import QueryJwtConfigAction from '@/app/actions/configs/jwt/query-jwt-config-action';

export const metadata: Metadata = {
  title: 'jwt config - youdeyiwu',
  description: 'jwt config page',
};

export default async function Page() {
  return <JwtConfig config={await QueryJwtConfigAction()} />;
}
