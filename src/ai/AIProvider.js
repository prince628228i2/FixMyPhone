import { getProvider } from './ProviderFactory';
export async function createActionPlan(input){ return getProvider().then(p=>p.plan(input)); }
