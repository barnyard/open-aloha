class Conference < ActiveRecord::Base
  validates_uniqueness_of :name
  has_and_belongs_to_many :participants
  
  def strtime
    self[:time].strftime("%H:%M")
  end
end
