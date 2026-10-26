local restored = 0

for i = 1, #KEYS do
    local exist = redis.call('EXISTS', KEYS[i])
    if exist == 1 then
        redis.call('INCRBY', KEYS[i], ARVG[i])
        restored = restored + 1
    end
end

return restored