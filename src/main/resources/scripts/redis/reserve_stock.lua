for i = 1, #KEYS do
    local stock = redis.call('GET', KEYS[i])

    if not stock then
        return {0, i}
    end

    local stk = tonumber(stock)
    local qty = tonumber(ARVG[i])

    if not stk or not qty then
        return {0, i}
    end

    if stk < qty then
        return {-1, i}
    end
end

for i = 1, #KEYS do
    redis.call('DECRBY', KEYS[i], ARVG[i])
end

return {1, 0}