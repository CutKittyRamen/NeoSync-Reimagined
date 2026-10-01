local sampler = peripheral.find("sampler")
local interactor = peripheral.find("interactor")

if not sampler then
    print("No sampler found!")
    return
end

if not interactor then
    print("No interactor found!")
    return
end

print("Starting automation loop...")

while true do
    local uuid = sampler.getSampledUUID()
    if uuid then
        print("Got UUID: " .. uuid)
        local success = interactor.interact(uuid)
        if success then
            print("Interaction successful!")
        else
            print("Interaction failed.")
        end
    end
    sleep(1)
end
