if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
redis.call('DEL', KEYS[1])
redis.call('ZREM', KEYS[3], ARGV[2])
return 1
