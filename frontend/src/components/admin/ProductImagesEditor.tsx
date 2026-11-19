import { useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import "./ProductImagesEditor.css";

type Props = {
  images: string[];
  onChange: (images: string[]) => void;
  disabled?: boolean;
  error?: string;
};

export function ProductImagesEditor({ images, onChange, disabled, error }: Props) {
  const [open, setOpen] = useState(false);
  return <div className="product-images-field">
    <div className="product-images-heading">
      <span>Product images ({images.length}/10)</span>
      <button type="button" className="button ghost" disabled={disabled}
        onClick={() => setOpen(true)}>Edit images</button>
    </div>
    <p className="muted">The first image is the main product image.</p>
    <div className="product-images-summary">
      {images.slice(0, 4).map((url, index) => <ImagePreview key={`${index}:${url}`} url={url} label={`Image ${index + 1}`} />)}
      {!images.length && <span>No images added.</span>}
    </div>
    {error && <p className="field-error" role="alert">{error}</p>}
    {open && <ImagesDialog initialImages={images}
      onCancel={() => setOpen(false)}
      onDone={(next) => { onChange(next); setOpen(false); }} />}
  </div>;
}

function ImagePreview({ url, label }: { url: string; label: string }) {
  const [failed, setFailed] = useState(false);
  return <div className="product-image-preview">
    {failed ? <span>Preview unavailable</span> :
      <img src={url} alt={label} referrerPolicy="no-referrer" onError={() => setFailed(true)} />}
  </div>;
}

function ImagesDialog({ initialImages, onCancel, onDone }: {
  initialImages: string[];
  onCancel: () => void;
  onDone: (images: string[]) => void;
}) {
  const [images, setImages] = useState(() => [...initialImages]);
  const [url, setUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState("");
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const inputId = useId();
  const errorId = useId();

  useEffect(() => {
    const dialog = dialogRef.current!;
    const previousFocus = document.activeElement;
    dialog.showModal();
    return () => {
      dialog.close();
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus();
    };
  }, []);

  function addImage() {
    const next = url.trim();
    if (images.length >= 10) { setError("Use at most 10 images."); return; }
    try {
      const parsed = new URL(next);
      if (parsed.protocol !== "https:" || !parsed.hostname || next.length > 2048) throw new Error();
    } catch {
      setError("Enter a complete HTTPS image URL, up to 2048 characters.");
      return;
    }
    if (images.includes(next)) { setError("That image URL is already in the list."); return; }
    setImages(current => [...current, next]);
    setUrl("");
    setError(null);
    setAnnouncement("Image added.");
  }

  function moveImage(index: number, offset: number) {
    const target = index + offset;
    if (target < 0 || target >= images.length) return;
    setImages(current => {
      const next = [...current];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
    setAnnouncement(`Image moved to position ${target + 1}.`);
  }

  return createPortal(<dialog ref={dialogRef} className="product-images-dialog"
    aria-labelledby={titleId} onCancel={event => { event.preventDefault(); onCancel(); }}>
    <form noValidate onSubmit={event => { event.preventDefault(); event.stopPropagation(); addImage(); }}>
      <header className="product-images-heading">
        <h2 id={titleId}>Edit images</h2>
        <button type="button" className="button ghost" aria-label="Cancel image changes" onClick={onCancel}>×</button>
      </header>
      <p>Paste image URLs, then use Earlier and Later to set their order.</p>
      <p className="muted">Done updates the product draft. Save the product to save these changes.</p>
      <label htmlFor={inputId}>Image URL</label>
      <div className="product-image-add">
        <input id={inputId} type="url" value={url} maxLength={2048}
          placeholder="https://example.com/product.jpg" autoFocus
          aria-invalid={Boolean(error)} aria-describedby={error ? errorId : undefined}
          onChange={event => { setUrl(event.target.value); setError(null); }} />
        <button type="submit" className="button" disabled={images.length >= 10}>Add image</button>
      </div>
      {error && <p id={errorId} className="field-error" role="alert">{error}</p>}
      <p role="status" className="product-image-status">{announcement}</p>
      {!images.length && <p className="product-images-empty">No images yet. Add your first image above.</p>}
      <ol className="product-images-list">
        {images.map((image, index) => <li key={image}>
          <ImagePreview url={image} label={`Product image ${index + 1}`} />
          <div className="product-image-info">
            <strong>{index === 0 ? "1 · Main image" : `${index + 1} · Gallery image`}</strong>
            <span className="product-image-url">{image}</span>
            <div className="product-image-actions">
              <button type="button" className="button ghost" disabled={index === 0}
                aria-label={`Move image ${index + 1} earlier`} onClick={() => moveImage(index, -1)}>← Earlier</button>
              <button type="button" className="button ghost" disabled={index === images.length - 1}
                aria-label={`Move image ${index + 1} later`} onClick={() => moveImage(index, 1)}>Later →</button>
              <button type="button" className="button danger" aria-label={`Remove image ${index + 1}`}
                onClick={() => { setImages(current => current.filter((_, i) => i !== index)); setError(null); setAnnouncement(`Image ${index + 1} removed.`); }}>Remove</button>
            </div>
          </div>
        </li>)}
      </ol>
      <footer className="product-images-footer">
        <button type="button" className="button ghost" onClick={onCancel}>Cancel</button>
        <button type="button" className="button" onClick={() => {
          if (url.trim()) { setError("Click Add image or clear the URL before clicking Done."); return; }
          onDone([...images]);
        }}>Done</button>
      </footer>
    </form>
  </dialog>, document.body);
}
