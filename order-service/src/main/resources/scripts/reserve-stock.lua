local stockKey = KEYS[1]
local quantity = tonumber(ARGV[1])

local current = tonumber(redis.call('GET', stockKey) or "0")

if current >= quantity then
    redis.call('DECRBY', stockKey, quantity)
    return 1
else
    return 0
end
