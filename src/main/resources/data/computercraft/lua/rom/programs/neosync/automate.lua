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

print("Waiting for player sample on Sampler front face...")

while true do
    local uuid = sampler.getSampledUUID()
    if uuid then
        print("Sampled UUID: " .. uuid)
        local success = interactor.interact(uuid)
        if success then
            print("Injected shell into constructor!")
        else
            print("Failed to inject (constructor busy or missing).")
        end
    end
    sleep(0.2)
end
