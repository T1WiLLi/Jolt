import Cookies from "../sections/fc/Cookies"
import Database from "../sections/fc/Database"
import ExternalApiRequest from "../sections/fc/ExternalApiRequest"
import FileUpload from "../sections/fc/FileUpload"
import Filters from "../sections/fc/Filters"
import Forms from "../sections/fc/Forms"
import Localization from "../sections/fc/Localization"
import Logging from "../sections/fc/Logging"
import TemplatingEngine from "../sections/fc/TemplatingEngine"

function FrameworkComponent() {
    return (
        <div className="flex flex-col gap-5">
            <Filters />
            <Logging />
            <Cookies />
            <Forms />
            <FileUpload />
            <ExternalApiRequest />
            <Database />
            <TemplatingEngine />
            <Localization />
        </div>
    )
}

export default FrameworkComponent