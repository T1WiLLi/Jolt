import Filters from "../sections/fc/Filters"
import Logging from "../sections/fc/Logging"

function FrameworkComponent() {
    return (
        <div className="flex flex-col gap-5">
            <Filters />
            <Logging />
        </div>
    )
}

export default FrameworkComponent