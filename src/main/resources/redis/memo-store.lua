if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
if ARGV[3] == '1' then
    redis.call('SET', KEYS[1], ARGV[2])
    if ARGV[6] == '1' then redis.call('ZREM', KEYS[3], ARGV[5])
    else redis.call('ZADD', KEYS[3], ARGV[4], ARGV[5]) end
else
    redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[7])
    redis.call('ZREM', KEYS[3], ARGV[5])
end
return 1
