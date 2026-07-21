type BannerProps = {
  message: string;
  visible: boolean;
};

export function FloatingMessage({ message, visible }: BannerProps) {

  return (
    <div className="background">
      <p className={`floating-message ${visible ? "show" : "hide"}`}>
        {message}
      </p>
    </div>
  )
}