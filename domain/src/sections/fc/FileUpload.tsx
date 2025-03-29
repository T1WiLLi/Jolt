// FileUpload.jsx
import { Upload, Code, Rocket } from 'lucide-react';
import CodeBlock from '../../lib/CodeBlock';

function FileUpload() {
    const exampleCode1 = `
post("/upload", ctx -> {
    JoltFile file = ctx.file("profilePicture");
    if (file == null) {
        return ctx.status(HttpStatus.BAD_REQUEST).text("No file uploaded");
    }

    // Access file metadata
    String fileName = file.getFileName();
    String contentType = file.getContentType();
    long size = file.getSize();

    // Log file details
    String message = "Uploaded file: " + fileName + ", Type: " + contentType + ", Size: " + size + " bytes";
    return ctx.text(message);
});
    `.trim();

    const exampleCode2 = `
post("/upload-and-save", ctx -> {
    JoltFile file = ctx.file("document");
    if (file == null) {
        return ctx.status(HttpStatus.BAD_REQUEST).text("No file uploaded");
    }

    // Save the file to the filesystem
    String savePath = "uploads/" + file.getFileName();
    file.save(savePath);

    return ctx.text("File saved to: " + savePath);
});
    `.trim();

    const exampleCode3 = `
get("/download-profile", ctx -> {
    JoltFile staticFile = JoltFile.fromStatic("profile.jpg");
    return ctx.download(staticFile, "profile.jpg");
});
    `.trim();
    return (
        <div id="file-uploads" className="bg-gray-900 text-gray-100 rounded-lg shadow-lg p-6 w-full max-w-8xl mx-auto sm:p-4">
            <div className="flex items-center mb-4">
                <Upload className="w-8 h-8 text-blue-400 mr-3" />
                <h1 className="text-3xl font-bold text-blue-400">File Uploads</h1>
            </div>

            <p className="text-gray-300 mb-6">
                File uploads are a common requirement in web applications, and Jolt makes it easy to handle them with the <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFile</code> class. Integrated with <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code>, you can access uploaded files, retrieve their metadata, save them to the filesystem, and trigger client-side downloads. Jolt also supports serving static files from the <code className="text-blue-400 bg-gray-800 px-1 rounded">static</code> resource directory.
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Key Features</h2>
            <ul className="space-y-3 mb-6">
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        File metadata: Access file details like <code className="text-blue-400 bg-gray-800 px-1 rounded">fileName</code>, <code className="text-blue-400 bg-gray-800 px-1 rounded">contentType</code>, and <code className="text-blue-400 bg-gray-800 px-1 rounded">size</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        In-memory storage: Store uploaded files in memory with <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFile</code> and access their data as a <code className="text-blue-400 bg-gray-800 px-1 rounded">byte[]</code> or <code className="text-blue-400 bg-gray-800 px-1 rounded">InputStream</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        File saving: Save uploaded files to the filesystem using <code className="text-blue-400 bg-gray-800 px-1 rounded">save(String path)</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Static file serving: Serve files from the <code className="text-blue-400 bg-gray-800 px-1 rounded">static</code> directory using <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFile.fromStatic()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        Client-side downloads: Trigger downloads with <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.download()</code>.
                    </span>
                </li>
                <li className="flex items-start">
                    <Code className="w-5 h-5 text-blue-400 mr-2 mt-1 flex-shrink-0" />
                    <span className="text-gray-300">
                        JoltContext integration: Access uploaded files with <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.file(String name)</code> and send files with <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.send(InputStream)</code>.
                    </span>
                </li>
            </ul>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Handling File Uploads</h2>
            <p className="text-gray-300 mb-4">
                Jolt simplifies file uploads through the <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code> class. Use <code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.file(String name)</code> to retrieve an uploaded file as a <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFile</code> instance, then access its metadata or data for processing.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Handling a file upload and accessing its metadata:
                </p>
                <CodeBlock code={exampleCode1} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.file(String name)</code>: Retrieves an uploaded file by its form field name.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getFileName()</code>: Gets the original file name.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getContentType()</code>: Gets the file’s MIME type.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getSize()</code>: Gets the file size in bytes.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getData()</code>: Gets the file data as a <code className="text-blue-400 bg-gray-800 px-1 rounded">byte[]</code>.</li>
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">getInputStream()</code>: Gets the file data as an <code className="text-blue-400 bg-gray-800 px-1 rounded">InputStream</code>.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Saving and Serving Files</h2>
            <p className="text-gray-300 mb-4">
                The <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltFile</code> class provides methods to save uploaded files to the filesystem. Use <code className="text-blue-400 bg-gray-800 px-1 rounded">save(String path)</code> to persist an uploaded file.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Saving an uploaded file:
                </p>
                <CodeBlock code={exampleCode2} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">save(String path)</code>: Saves the file to the specified path.</li>
                </ul>
            </p>

            <h2 className="text-xl font-semibold text-gray-200 mb-3">Triggering a Client-Side Download</h2>
            <p className="text-gray-300 mb-4">
                To trigger a file download on the client, use the <code className="text-blue-400 bg-gray-800 px-1 rounded">download()</code> method provided by <code className="text-blue-400 bg-gray-800 px-1 rounded">JoltContext</code>. This method sets the <code className="text-blue-400 bg-gray-800 px-1 rounded">Content-Disposition</code> header to <code className="text-blue-400 bg-gray-800 px-1 rounded">attachment</code> and sends the file data, prompting the browser to download the file.
            </p>
            <div className="mb-6">
                <p className="text-gray-300 mb-2">
                    Example: Triggering a download for a static file:
                </p>
                <CodeBlock code={exampleCode3} language="js" />
            </div>
            <p className="text-gray-300 mb-4">
                Key methods:
                <ul className="list-disc list-inside ml-4 mt-2 space-y-1">
                    <li><code className="text-blue-400 bg-gray-800 px-1 rounded">ctx.download(JoltFile file, String filename)</code>: Triggers a file download by setting the appropriate headers and sending the file data.</li>
                </ul>
            </p>
            <div className="bg-gray-800 p-4 rounded-md flex items-start">
                <Rocket className="w-6 h-6 text-blue-400 mr-3 flex-shrink-0" />
                <div>
                    <h3 className="text-lg font-medium text-gray-100 mb-2">Next Steps</h3>
                    <p className="text-gray-400">
                        With file uploads and downloads in place, explore the{' '}
                        <a href="#external-api-requests" className="text-blue-400 hover:underline">
                            External Api Requests
                        </a>{' '}
                        section to learn how to make requests to external APIs.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default FileUpload;