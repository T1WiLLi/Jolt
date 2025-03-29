import Cookies from "../sections/fc/Cookies"
import FileUpload from "../sections/fc/FileUpload"
import Filters from "../sections/fc/Filters"
import Forms from "../sections/fc/Forms"
import Logging from "../sections/fc/Logging"

function FrameworkComponent() {
    return (
        <div className="flex flex-col gap-5">
            <Filters />
            <Logging />
            <Cookies />
            <Forms />
            <FileUpload />
        </div>
    )
}

export default FrameworkComponent