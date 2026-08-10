type BannerProps = {
  className: string
  message: string;
  visible: boolean;
};

export function FloatingMessage({ className, message, visible }: BannerProps) {

  return (
    <div className="background">
      <p className={`${className} floating-message ${visible ? "show" : "hide"}`}>
        {message}
      </p>
    </div>
  )
}