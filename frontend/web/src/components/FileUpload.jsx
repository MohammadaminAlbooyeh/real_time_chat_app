import { useState, useRef } from 'react'
import { api } from '../services/api'
import { isImageFile, formatFileSize } from '../utils/helpers'

export default function FileUpload({ roomId, onFileAttached, onClose }) {
  const [files, setFiles] = useState([])
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState(null)
  const inputRef = useRef(null)

  const handleSelect = (e) => {
    const selected = Array.from(e.target.files)
    setFiles(prev => [...prev, ...selected])
  }

  const removeFile = (idx) => {
    setFiles(prev => prev.filter((_, i) => i !== idx))
  }

  const handleUpload = async () => {
    if (files.length === 0) return
    setUploading(true)
    setError(null)

    try {
      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        if (roomId) formData.append('room_id', roomId)
        const result = await api.upload('/upload', formData)
        onFileAttached(result)
      }
      setFiles([])
      onClose?.()
    } catch (err) {
      setError(err.message || 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="file-upload">
      <div className="file-upload-header">
        <h4>Attach Files</h4>
        <button className="btn-icon" onClick={onClose}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
      <input
        ref={inputRef}
        type="file"
        multiple
        onChange={handleSelect}
        className="file-input-hidden"
      />
      <button className="btn btn-secondary btn-sm" onClick={() => inputRef.current?.click()}>
        Choose Files
      </button>
      {files.length > 0 && (
        <div className="file-list">
          {files.map((file, i) => (
            <div key={i} className="file-item">
              {isImageFile(file.type) && (
                <img src={URL.createObjectURL(file)} alt="" className="file-thumb" />
              )}
              <div className="file-info">
                <span className="file-name">{file.name}</span>
                <span className="file-size">{formatFileSize(file.size)}</span>
              </div>
              <button className="btn-icon btn-icon-sm" onClick={() => removeFile(i)}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          ))}
          <button className="btn btn-primary btn-sm" onClick={handleUpload} disabled={uploading}>
            {uploading ? 'Uploading...' : `Upload (${files.length})`}
          </button>
        </div>
      )}
      {error && <p className="error-text">{error}</p>}
    </div>
  )
}
