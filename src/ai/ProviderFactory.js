import { GeminiProvider } from './GeminiProvider';
import { OpenAIProvider } from './OpenAIProvider';
import { NativeModules } from 'react-native';
export async function getProvider() {
 const cfg=await NativeModules.SecureKeyStore?.getProviderConfig?.();
 if(!cfg?.provider||!cfg?.key) throw new Error('Configure an AI provider in Settings first');
 return cfg.provider==='openai' ? new OpenAIProvider(cfg.key,cfg.model||'gpt-4o-mini') : new GeminiProvider(cfg.key);
}
