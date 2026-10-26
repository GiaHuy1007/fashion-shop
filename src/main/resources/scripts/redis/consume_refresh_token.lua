local data = redis.call('HGETALL', KEYS[1])

if #data == 0 then return nil end

redis.call('DEL', KEYS[1])

return data