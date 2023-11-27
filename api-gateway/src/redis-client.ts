export interface RedisClient {
  isReady: boolean

  get(key: string): Promise<string | null>

  set(
    key: string,
    value: string,
    options: { EX: number }
  ): Promise<string | null>

  del(key: string | string[]): Promise<number>

  expire(key: string, seconds: number): Promise<boolean>

  ping(): Promise<string>
}

export async function assertRedisConnection(
  client: RedisClient
): Promise<void> {
  if (!client.isReady) {
    throw new Error('not connected to redis')
  }
  await client.ping()
}
